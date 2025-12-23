pipeline {
  agent any
  environment {
    TF_IN_AUTOMATION = '1'
    TF_CLI_ARGS = '-no-color'
  }
  options {
    skipDefaultCheckout true
  }
  stages {
    stage('Init') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
            sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
          ])
           {
            sh '''
              echo "Initializing Terraform..."
              git config --global http.sslCAInfo /etc/ssl/certs/ca-certificates.crt; terraform init
            '''
          }
        }
      }
    }

    stage('Inspect TFVARS') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          sh "echo 'Displaying vars for branch: ${branch}'; if [ -f ${branch}.tfvars ]; then cat ${branch}.tfvars; else echo '${branch}.tfvars not found'; fi"
        }
      }
    }

    stage('Plan') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
            sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
          ]) {
            sh "terraform plan -var-file=${branch}.tfvars -out=plan-${branch}.tfplan"
            sh "terraform show -no-color plan-${branch}.tfplan"
          }
        }
      }
    }

    stage('Validate & Apply') {
      when { branch 'dev' }
      steps {
        script {
          input message: 'Approve apply to dev?', ok: 'Apply'
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
            sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
          ]) {
            sh "terraform apply -auto-approve plan-${branch}.tfplan"
          }
        }
      }
    }
  }
}
