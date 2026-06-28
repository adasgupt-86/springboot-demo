pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    stages {

        stage('Environment') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
