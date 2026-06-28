pipeline {

    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'qa', 'uat', 'prod'],
            description: 'Target deployment environment'
        )
    }

    environment {

        APP_NAME = "springboot-demo"
        DOCKER_IMAGE = "adasgupt86/springboot-demo"
        IMAGE_TAG = "${BUILD_NUMBER}"

    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package'
            }
        }

        stage('SonarQube Scan') {
            steps {
                script {
                    def scannerHome = tool 'sonar-scanner'

                    withSonarQubeEnv('sonarqube') {
                        sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=Sonar-Qube-testing \
                        -Dsonar.projectName=Sonar-Qube-testing \
                        -Dsonar.sources=. \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.exclusions=**/*.js,**/*.ts,**/*.css
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Manual Approval') {
            steps {
                input message: "Approve deployment to ${params.ENVIRONMENT}?",
                      ok: 'Approve'
            }
        }

        stage('Trivy File System Scan') {
            steps {
                sh '''
                trivy fs \
                  --severity HIGH,CRITICAL \
                  --exit-code 0 \
                  .
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                docker build \
                  -t ${DOCKER_IMAGE}:${IMAGE_TAG} \
                  -t ${DOCKER_IMAGE}:latest \
                  .
                """
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh """
                trivy image \
                  --severity HIGH,CRITICAL \
                  --exit-code 0 \
                  --no-progress \
                  ${DOCKER_IMAGE}:${IMAGE_TAG}
                """
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                    echo "$DOCKER_PASS" | docker login \
                      -u "$DOCKER_USER" \
                      --password-stdin
                    '''
                }
            }
        }

        stage('Docker Push') {
            steps {
                sh """
                docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                docker push ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Verify Helm') {
            steps {
                sh '''
                echo "PATH=$PATH"
                which helm || true
                helm version
                '''
            }
        }

        stage('Helm Lint') {
            steps {
                sh 'helm lint helm'
            }
        }

        stage('Helm Package') {
            steps {
                sh 'helm package helm'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def envName = params.ENVIRONMENT
                    sh """
                    helm upgrade --install ${APP_NAME} helm \
                      -n ${envName} --create-namespace \
                      -f values-${envName}.yaml \
                      --set image.repository=${DOCKER_IMAGE} \
                      --set image.tag=${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Verify Rollout') {
            steps {
                script {
                    def envName = params.ENVIRONMENT

                    sh """
                    kubectl rollout status deployment/${APP_NAME} -n ${envName}
                    """
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    def envName = params.ENVIRONMENT

                    sh """
                
                    NODEPORT=\$(kubectl get svc ${APP_NAME} -n ${envName} -o jsonpath='{.spec.ports[0].nodePort}')
                    NODEIP=\$(kubectl get nodes -o wide | awk 'NR==2 {print \$6}')

                    echo "Testing http://\$NODEIP:\$NODEPORT/api/health"

                    curl -f http://\$NODEIP:\$NODEPORT/api/health
                    """
                }
            }
        }

    }

    post {

        always {
            junit '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }

        success {
            echo "Pipeline SUCCESS"
        }

        failure {
            echo "Pipeline FAILED"
        }

        cleanup {
            sh 'docker logout || true'
        }

    }
}
