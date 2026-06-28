pipeline {

    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    environment {
        APP_NAME = "springboot-demo"
        DOCKER_IMAGE = "adasgupt86/springboot-demo"
    }

    options {
        timestamps()
        disableConcurrentBuilds()

        buildDiscarder(logRotator(
            numToKeepStr: '20',
            artifactNumToKeepStr: '10'
        ))
    }

    stages {

        stage('Checkout') {
            steps {
                echo "========== CHECKOUT =========="
                checkout scm
            }
        }

        stage('Environment Info') {
            steps {
                sh '''
                    echo "========== JAVA =========="
                    java -version

                    echo
                    echo "========== MAVEN =========="
                    mvn -version

                    echo
                    echo "========== DOCKER =========="
                    docker --version

                    echo
                    pwd
                    ls -la
                '''
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package'
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
            }
        }

        stage('SonarQube Scan') {

            steps {

                script {

                    def scannerHome = tool 'sonar-scanner'

                    withSonarQubeEnv('sonarqube') {

                        sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=springboot-demo \
                        -Dsonar.projectName="Spring Boot Demo" \
                        -Dsonar.sources=src \
                        -Dsonar.java.binaries=target/classes
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {

            steps {

                timeout(time: 5, unit: 'MINUTES') {

                    waitForQualityGate abortPipeline: true

                }

            }

        }

        stage('Manual Approval') {

            steps {

                input(
                    message: 'Approve Security Scan & Docker Build?',
                    ok: 'Approve'
                )

            }

        }

        stage('Trivy Filesystem Scan') {

            steps {

                sh '''
                    trivy fs \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        .
                '''

            }

        }

        stage('Docker Build') {

            steps {

                sh """
                    docker build \
                    -t ${DOCKER_IMAGE}:${BUILD_NUMBER} \
                    -t ${DOCKER_IMAGE}:latest .
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
                    ${DOCKER_IMAGE}:${BUILD_NUMBER}
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
                    docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                    docker push ${DOCKER_IMAGE}:latest
                """

            }

        }

    }

    post {

        success {

            echo "Pipeline completed successfully."

        }

        failure {

            echo "Pipeline failed."

        }

        always {

            junit allowEmptyResults: true,
                  testResults: '**/target/surefire-reports/*.xml'

            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true

            sh 'docker logout || true'

            // Enable later
            // cleanWs()
        }

    }

}
