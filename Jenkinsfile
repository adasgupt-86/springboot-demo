pipeline {
    agent any

    environment {
        APP_NAME = "springboot-demo"
        IMAGE_NAME = "adasgupt86/springboot-demo"
        IMAGE_TAG = "${BUILD_NUMBER}"
        }

        parameters {
            string(
                name: 'GIT_BRANCH',
                defaultValue: 'feature/canary-v2',
                description: 'Git Branch'
            )
        }

    stages {

        stage('Checkout') {
            steps {
                git branch: "${params.GIT_BRANCH}",
                    credentialsId: 'github-pat',
                    url: 'https://github.com/adasgupt-86/springboot-demo.git'
                    changelog: false,
                    poll: false
            }
        }

        stage('Verify Tools') {
            steps {
                sh '''
                java -version
                mvn -version
                git --version
                docker --version
                helm version
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                        -Dsonar.projectKey=springboot-demo \
                        -Dsonar.projectName=springboot-demo
                    '''
                }
            }
        }

        stage('Filesystem Scan') {
            steps {
                sh '''
                docker run --rm \
                    -v $WORKSPACE:/workspace \
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    aquasec/trivy:0.66.0 \
                    fs \
                    --severity HIGH,CRITICAL \
                    /workspace
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                docker build \
                -t ${IMAGE_NAME}:${IMAGE_TAG} \
                -t ${IMAGE_NAME}:latest .
                '''
            }
        }

        stage('Image Scan') {
            steps {
                sh '''
                docker run --rm \
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    aquasec/trivy:0.66.0 \
                    image \
                    --severity HIGH,CRITICAL \
                    adasgupt86/springboot-demo:${BUILD_NUMBER}
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-cred',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {

                    sh '''
                    echo "$DOCKER_PASS" | docker login \
                        -u "$DOCKER_USER" \
                        --password-stdin

                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('Helm Lint') {
            steps {
                sh '''
                helm lint ./springboot-demo
                '''
            }
        }

        stage('Helm Deploy') {
            steps {
                withCredentials([
                    file(credentialsId: 'kubeconfig-cred',
                        variable: 'KUBECONFIG')
                ]) {

                    sh '''
                    echo "Deploying application using Helm"

                    helm upgrade --install springboot-demo \
                    ./springboot-demo \
                    --namespace springboot-demo \
                    --create-namespace

                    kubectl rollout status deployment/springboot-demo \
                    -n springboot-demo \
                    --timeout=120s

                    kubectl get pods -n springboot-demo
                    '''
                }
            }
        }

        stage('Docker Cleanup') {
            steps {
                sh '''
                docker builder prune -af
                docker image prune -f
                docker container prune -f
            '''
            }
        }

    }

    post {

        always {
            cleanWs()
        }

        success {

            emailext(
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",

                body: """
                <html>

                <body>

                <img src="https://www.jenkins.io/images/logos/jenkins/jenkins.png"
                 width="120"/>

                <h2 style="color:green;">
                Jenkins Build SUCCESS
                </h2>


                <table border="1" cellpadding="8">

                <tr>
                <td><b>Job Name</b></td>
                <td>${env.JOB_NAME}</td>
                </tr>


                <tr>
                <td><b>Build Number</b></td>
                <td>${env.BUILD_NUMBER}</td>
                </tr>


                <tr>
                <td><b>Status</b></td>
                <td>SUCCESS</td>
                </tr>


                <tr>
                <td><b>Build URL</b></td>
                <td>
                <a href="${env.BUILD_URL}">
                ${env.BUILD_URL}
                </a>
                </td>
                </tr>

                </table>


                <br>

                Application:
                springboot-demo

                <br>

                Deployment:
                Kubernetes via Helm

                </body>

                </html>
                """,

                mimeType: 'text/html',

                to: 'abhishek.dasgupta@gmail.com'
            )

        }


        failure {

            emailext(

                subject:
                "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",


                body: """

                <html>

                <body>

                <img src="https://www.jenkins.io/images/logos/jenkins/jenkins.png"
                 width="120"/>


                <h2 style="color:red;">
                Jenkins Build FAILED
                </h2>


                <table border="1" cellpadding="8">


                <tr>
                <td><b>Job Name</b></td>
                <td>${env.JOB_NAME}</td>
                </tr>


                <tr>
                <td><b>Build Number</b></td>
                <td>${env.BUILD_NUMBER}</td>
                </tr>


                <tr>
                <td><b>Status</b></td>
                <td>FAILED</td>
                </tr>


                <tr>
                <td><b>Console URL</b></td>
                <td>
                <a href="${env.BUILD_URL}">
                ${env.BUILD_URL}
                </a>
                </td>
                </tr>


                </table>


                <br>

                Please check Jenkins console logs.

                </body>

                </html>

                """,

                mimeType: 'text/html',

                to: 'abhishek.dasgupta@gmail.com'

         )

        }

    }
}
