pipeline {

    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    environment {
        APP_NAME = "springboot-demo"
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
                echo '========== CHECKOUT =========='
                checkout scm
            }
        }

        stage('Environment Info') {
            steps {
                echo '========== ENVIRONMENT =========='

                sh '''
                    echo "JAVA VERSION"
                    java -version

                    echo
                    echo "MAVEN VERSION"
                    mvn -version

                    echo
                    echo "WORKSPACE"
                    pwd
                '''
            }
        }

        stage('Compile') {
            steps {
                echo '========== COMPILE =========='

                sh '''
                    mvn clean compile
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                echo '========== UNIT TESTS =========='

                sh '''
                    mvn test
                '''
            }
        }

        stage('Package') {
            steps {
                echo '========== PACKAGE =========='

                sh '''
                    mvn package
                '''
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
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
                    message: 'Approve Docker Build & Push?',
                    ok: 'Approve'
                )

            }
        }

    }

    post {

        success {
            echo 'Pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed.'
        }

        always {

            junit allowEmptyResults: true,
                  testResults: '**/target/surefire-reports/*.xml'

            cleanWs()

        }
    }

}
