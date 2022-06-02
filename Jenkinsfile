pipeline {

    agent {
        label 'agent any' //The id of the slave/agent where the build should be executed, if it doesn't matter use "agent any" instead.
    }

    triggers {
        cron('H */8 * * *') //regular builds
        pollSCM('* * * * *') //polling for changes, here once a minute
    }

    stages {
        stage('Build project') {
            steps {
                script {
                    sh './gradlew clean build' //run a gradle task
                }
            }
        }
//         stage('Publish Artifact to Nexus') {
//             steps {
//                 sh './gradlew publish --no-daemon'
//             }
//         }
    }
    post {
//         always { //Send an email to the person that broke the build
//             step([$class                  : 'Mailer',
//                   notifyEveryUnstableBuild: true,
//                   recipients              : [emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']])].join(' ')])
//         }
    }
}
