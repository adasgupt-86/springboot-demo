pipeline {
    agent any

    environment {
        APP_NAME = "springboot-demo"
        IMAGE_NAME = "adasgupt86/springboot-demo"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                sh '''
                java -version
                mvn -version
                git --version
                docker --version
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
            echo "Pipeline completed successfully."
        }

        failure {
            echo "Pipeline failed."
        }

    }

}
