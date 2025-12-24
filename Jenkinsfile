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
      stage('Checkout') {
        steps {
          script {
            echo 'Checking out repository...'
            checkout scm
            sh 'echo Workspace listing:'
            sh 'ls -la'
          }
        }
      }
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
                ls -la
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
          input message: 'Approve apply to branch dev?', ok: 'Apply'
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

    stage('Provisioning & Output Capture') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
            sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
          ]) {
            sh '''
              echo "Capturing Terraform outputs..."
              INSTANCE_PUBLIC_IP=$(terraform output -raw instance_public_ip 2>/dev/null || echo "")
              INSTANCE_ID=$(terraform output -raw instance_id 2>/dev/null || echo "")
              
              if [ -z "$INSTANCE_PUBLIC_IP" ] || [ -z "$INSTANCE_ID" ]; then
                echo "Error: Failed to capture Terraform outputs"
                exit 1
              fi
              
              echo "Instance Public IP: $INSTANCE_PUBLIC_IP"
              echo "Instance ID: $INSTANCE_ID"
              
              # Export to Jenkins environment variables
              echo "INSTANCE_IP=$INSTANCE_PUBLIC_IP" >> $WORKSPACE/env.properties
              echo "INSTANCE_ID=$INSTANCE_ID" >> $WORKSPACE/env.properties
            '''
            load "$WORKSPACE/env.properties"
          }
        }
      }
    }

    stage('Dynamic Inventory Management') {
      steps {
        script {
          sh '''
            echo "Creating dynamic inventory file..."
            cat > dynamic_inventory.ini <<EOF
[webservers]
${INSTANCE_IP} ansible_user=ec2-user ansible_ssh_private_key_file=${SSH_KEY} ansible_python_interpreter=/usr/bin/python3
EOF
            
            echo "Dynamic inventory created:"
            cat dynamic_inventory.ini
            
            # Verify the file format
            if grep -q "^\[webservers\]" dynamic_inventory.ini && grep -q "ansible_user=" dynamic_inventory.ini; then
              echo "✓ Dynamic inventory file correctly formatted for Ansible"
            else
              echo "✗ Dynamic inventory file format incorrect"
              exit 1
            fi
          '''
        }
      }
    }

    stage('AWS Health Status Verification') {
      steps {
        script {
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')
          ]) {
            sh '''
              echo "Waiting for instance to pass health checks..."
              aws ec2 wait instance-status-ok --instance-ids ${INSTANCE_ID} --region us-east-1
              
              if [ $? -eq 0 ]; then
                echo "✓ EC2 instance is healthy and ready"
              else
                echo "✗ Instance health check failed"
                exit 1
              fi
            '''
          }
        }
      }
    }

    stage('Splunk Installation & Testing') {
      steps {
        script {
          sh '''
            echo "Running Splunk installation playbook..."
            ansible-playbook -i dynamic_inventory.ini playbooks/splunk.yml
            
            if [ $? -ne 0 ]; then
              echo "✗ Splunk installation failed"
              exit 1
            fi
          '''
          
          sh '''
            echo "Running Splunk test playbook..."
            ansible-playbook -i dynamic_inventory.ini playbooks/test-splunk.yml
            
            if [ $? -ne 0 ]; then
              echo "✗ Splunk verification failed"
              exit 1
            fi
            
            echo "✓ Splunk installation and verification successful"
          '''
        }
      }
    }

    stage('Validate Destroy') {
      steps {
        script {
          input message: 'Approve infrastructure destruction?', ok: 'Destroy'
        }
      }
    }

    stage('Destroy') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
          withCredentials([
            usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
            sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
          ]) {
            sh '''
              echo "Destroying infrastructure..."
              terraform destroy -auto-approve -var-file=${branch}.tfvars
              
              if [ $? -eq 0 ]; then
                echo "✓ Infrastructure destroyed successfully"
              else
                echo "✗ Infrastructure destruction failed"
                exit 1
              fi
            '''
          }
        }
      }
    }
  }

  post {
    always {
      script {
        echo "Cleaning up..."
        
        // Delete dynamic inventory file
        sh '''
          if [ -f dynamic_inventory.ini ]; then
            rm -f dynamic_inventory.ini
            echo "✓ Deleted dynamic_inventory.ini"
          fi
        '''
        
        // Clean up environment properties file
        sh '''
          if [ -f $WORKSPACE/env.properties ]; then
            rm -f $WORKSPACE/env.properties
            echo "✓ Deleted environment properties file"
          fi
        '''
      }
    }
    failure {
      script {
        echo "Pipeline failed. Attempting automatic infrastructure cleanup..."
        def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
        withCredentials([
          usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
          sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
        ]) {
          sh '''
            echo "Triggering automatic terraform destroy on pipeline failure..."
            terraform destroy -auto-approve -var-file=${branch}.tfvars || true
            echo "Cleanup attempt completed"
          '''
        }
      }
    }
    aborted {
      script {
        echo "Pipeline aborted. Attempting automatic infrastructure cleanup..."
        def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'dev'
        withCredentials([
          usernamePassword(credentialsId: 'AWS_CRED_ID', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
          sshUserPrivateKey(credentialsId: 'SSH_CRED_ID', keyFileVariable: 'SSH_KEY')
        ]) {
          sh '''
            echo "Triggering automatic terraform destroy on pipeline abort..."
            terraform destroy -auto-approve -var-file=${branch}.tfvars || true
            echo "Cleanup attempt completed"
          '''
        }
      }
    }
  }
}
