pipeline {
    agent any
    
    // Task 2: Pipeline Environment & Credentials (20 Marks)
    environment {
        TF_IN_AUTOMATION = 'true'
        TF_CLI_ARGS = '-no-color'
        // AWS credentials will be injected securely using your existing credential
        AWS_CREDENTIAL = 'Devops-project-id'
        SSH_CRED_ID = 'ssh-private-key'
        PATH = "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:${env.PATH}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Checked out branch: ${env.BRANCH_NAME}"
            }
        }
        
        // Task 3: Initialization & Variable Inspection (20 Marks)
        stage('Terraform Initialization') {
            steps {
                withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                    script {
                        echo "Initializing Terraform for branch: ${env.BRANCH_NAME}"
                        sh '/bin/bash -c "terraform init"'
                        
                        // Display the contents of the branch-specific tfvars file
                        echo "Displaying ${env.BRANCH_NAME}.tfvars configuration:"
                        sh """#!/bin/bash
                            if [ -f ${env.BRANCH_NAME}.tfvars ]; then
                                echo "==== Contents of ${env.BRANCH_NAME}.tfvars ===="
                                cat ${env.BRANCH_NAME}.tfvars
                                echo "=============================================="
                            else
                                echo "Warning: ${env.BRANCH_NAME}.tfvars not found!"
                                exit 1
                            fi
                        """
                    }
                }
            }
        }
        
        // Task 4: Branch-Specific Terraform Planning (20 Marks)
        stage('Terraform Plan') {
            steps {
                withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                    script {
                        echo "Generating Terraform plan for ${env.BRANCH_NAME} environment"
                        sh """#!/bin/bash
                            terraform plan \
                                -var-file=${env.BRANCH_NAME}.tfvars \
                                -out=${env.BRANCH_NAME}.tfplan
                        """
                        echo "Terraform plan generated successfully!"
                    }
                }
            }
        }
        
        // Task 5: Conditional Manual Approval Gate (20 Marks)
        stage('Validate Apply') {
            when {
                branch 'dev'
            }
            steps {
                script {
                    echo "Requesting approval for dev environment deployment..."
                    def userInput = input(
                        id: 'ApprovalGate',
                        message: 'Do you want to apply this Terraform plan to the dev environment?',
                        parameters: [
                            choice(
                                name: 'APPROVAL',
                                choices: ['Approve', 'Reject'],
                                description: 'Select Approve to proceed with terraform apply'
                            )
                        ]
                    )
                    
                    if (userInput == 'Approve') {
                        echo "Deployment approved! Proceeding to apply..."
                    } else {
                        error "Deployment rejected by user. Aborting pipeline."
                    }
                }
            }
        }
        
        // BYOD-3 Task 1: Provisioning (20 Marks)
        stage('Terraform Apply') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                    script {
                        echo "Applying Terraform with auto-approve for ${env.BRANCH_NAME} environment"
                        sh """#!/bin/bash
                            terraform apply \
                                -auto-approve \
                                -var-file=${env.BRANCH_NAME}.tfvars
                        """
                        echo "Infrastructure deployed successfully!"
                    }
                }
            }
        }
        
        // BYOD-3 Task 1: Output Capture (20 Marks)
        stage('Capture Outputs') {
            when {
                branch 'dev'
            }
            steps {
                script {
                    echo "Capturing instance_public_ip and instance_id from Terraform outputs..."
                    
                    env.INSTANCE_IP = sh(
                        script: 'terraform output -raw ec2_public_ip',
                        returnStdout: true
                    ).trim()
                    
                    env.INSTANCE_ID = sh(
                        script: 'terraform output -raw ec2_instance_id',
                        returnStdout: true
                    ).trim()
                    
                    echo "✅ Captured INSTANCE_IP: ${env.INSTANCE_IP}"
                    echo "✅ Captured INSTANCE_ID: ${env.INSTANCE_ID}"
                }
            }
        }
        
        // BYOD-3 Task 2: Dynamic Inventory Management (20 Marks)
        stage('Create Dynamic Inventory') {
            when {
                branch 'dev'
            }
            steps {
                script {
                    echo "Creating dynamic_inventory.ini file..."
                    sh """#!/bin/bash
                        cat > dynamic_inventory.ini << EOF
[splunk_servers]
${env.INSTANCE_IP} ansible_user=ubuntu ansible_ssh_common_args='-o StrictHostKeyChecking=no'
EOF
                    """
                    
                    echo "Dynamic inventory file created successfully!"
                    sh 'cat dynamic_inventory.ini'
                }
            }
        }
        
        // BYOD-3 Task 3: AWS Health Status Verification (20 Marks)
        stage('AWS Health Check') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                    script {
                        echo "Waiting for instance ${env.INSTANCE_ID} to pass health checks..."
                        sh """#!/bin/bash
                            aws ec2 wait instance-status-ok \
                                --instance-ids ${env.INSTANCE_ID} \
                                --region us-east-1
                        """
                        echo "✅ EC2 instance health checks passed successfully!"
                    }
                }
            }
        }
        
        // BYOD-3 Task 4: Splunk Installation & Testing (20 Marks)
        stage('Configure Splunk') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CRED_ID, keyFileVariable: 'SSH_KEY')]) {
                    script {
                        echo "Installing Splunk using Ansible..."
                        sh """
                            export ANSIBLE_HOST_KEY_CHECKING=False
                            ansible-playbook playbooks/splunk.yml \
                                -i dynamic_inventory.ini \
                                --private-key=${SSH_KEY} \
                                --ssh-common-args='-o StrictHostKeyChecking=no'
                        """
                        echo "Splunk installation completed!"
                    }
                }
            }
        }
        
        stage('Test Splunk') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CRED_ID, keyFileVariable: 'SSH_KEY')]) {
                    script {
                        echo "Testing Splunk installation..."
                        sh """
                            export ANSIBLE_HOST_KEY_CHECKING=False
                            ansible-playbook playbooks/test-splunk.yml \
                                -i dynamic_inventory.ini \
                                --private-key=${SSH_KEY} \
                                --ssh-common-args='-o StrictHostKeyChecking=no'
                        """
                        echo "✅ Splunk service is active and reachable!"
                        echo "🌐 Access Splunk at: http://${env.INSTANCE_IP}:8000"
                        echo "👤 Username: admin | Password: SplunkAdmin123!"
                    }
                }
            }
        }
        
        // BYOD-3 Task 5: Infrastructure Destruction & Post-Build Actions (20 Marks)
        stage('Validate Destroy') {
            when {
                branch 'dev'
            }
            steps {
                script {
                    echo "Requesting approval for infrastructure destruction..."
                    def destroyInput = input(
                        id: 'DestroyGate',
                        message: 'Do you want to destroy the infrastructure?',
                        parameters: [
                            choice(
                                name: 'DESTROY_APPROVAL',
                                choices: ['Skip', 'Destroy'],
                                description: 'Select Destroy to tear down the infrastructure'
                            )
                        ]
                    )
                    
                    env.DESTROY_APPROVED = (destroyInput == 'Destroy') ? 'true' : 'false'
                    
                    if (env.DESTROY_APPROVED == 'true') {
                        echo "Destruction approved! Proceeding to destroy stage..."
                    } else {
                        echo "Destruction skipped by user. Infrastructure will remain active."
                    }
                }
            }
        }
        
        stage('Destroy Infrastructure') {
            when {
                allOf {
                    branch 'dev'
                    environment name: 'DESTROY_APPROVED', value: 'true'
                }
            }
            steps {
                withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                    script {
                        echo "Destroying infrastructure for ${env.BRANCH_NAME} environment..."
                        sh """#!/bin/bash
                            terraform destroy \
                                -auto-approve \
                                -var-file=${env.BRANCH_NAME}.tfvars
                        """
                        echo "Infrastructure destroyed successfully!"
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Pipeline completed successfully for branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "❌ Pipeline failed for branch: ${env.BRANCH_NAME}"
            echo "Initiating automatic cleanup..."
            
            // Delete dynamic_inventory.ini first
            script {
                try {
                    if (fileExists('dynamic_inventory.ini')) {
                        echo "Deleting dynamic_inventory.ini..."
                        sh 'rm -f dynamic_inventory.ini'
                        echo "✅ Dynamic inventory file deleted"
                    }
                } catch (Exception e) {
                    echo "Failed to delete dynamic_inventory.ini: ${e.message}"
                }
            }
            
            // Automatic destroy on failure
            script {
                try {
                    if (env.INSTANCE_ID && env.BRANCH_NAME == 'dev') {
                        withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                            echo "🔥 Running terraform destroy due to pipeline failure..."
                            sh """#!/bin/bash
                                terraform destroy \
                                    -auto-approve \
                                    -var-file=${env.BRANCH_NAME}.tfvars
                            """
                            echo "✅ Infrastructure destroyed successfully"
                        }
                    }
                } catch (Exception e) {
                    echo "⚠️ Terraform destroy failed: ${e.message}"
                    echo "Please manually destroy the infrastructure"
                }
            }
        }
        aborted {
            echo "⚠️ Pipeline aborted for branch: ${env.BRANCH_NAME}"
            echo "Initiating automatic cleanup..."
            
            // Delete dynamic_inventory.ini first
            script {
                try {
                    if (fileExists('dynamic_inventory.ini')) {
                        echo "Deleting dynamic_inventory.ini..."
                        sh 'rm -f dynamic_inventory.ini'
                        echo "✅ Dynamic inventory file deleted"
                    }
                } catch (Exception e) {
                    echo "Failed to delete dynamic_inventory.ini: ${e.message}"
                }
            }
            
            // Automatic destroy on abort
            script {
                try {
                    if (env.INSTANCE_ID && env.BRANCH_NAME == 'dev') {
                        withCredentials([aws(credentialsId: env.AWS_CREDENTIAL)]) {
                            echo "🔥 Running terraform destroy due to pipeline abort..."
                            sh """#!/bin/bash
                                terraform destroy \
                                    -auto-approve \
                                    -var-file=${env.BRANCH_NAME}.tfvars
                            """
                            echo "✅ Infrastructure destroyed successfully"
                        }
                    }
                } catch (Exception e) {
                    echo "⚠️ Terraform destroy failed: ${e.message}"
                    echo "Please manually destroy the infrastructure"
                }
            }
        }
        always {
            script {
                // Delete dynamic_inventory.ini file if still exists
                try {
                    if (fileExists('dynamic_inventory.ini')) {
                        echo "Deleting dynamic_inventory.ini..."
                        sh 'rm -f dynamic_inventory.ini'
                        echo "✅ Dynamic inventory file deleted"
                    }
                } catch (Exception e) {
                    echo "Failed to delete dynamic_inventory.ini: ${e.message}"
                }
                
                // Clean workspace AFTER destroy operations
                try {
                    cleanWs(
                        deleteDirs: true,
                        patterns: [
                            [pattern: '*.tfplan', type: 'INCLUDE'],
                            [pattern: 'dynamic_inventory.ini', type: 'INCLUDE']
                        ]
                    )
                    echo "✅ Workspace cleaned"
                } catch (Exception e) {
                    echo "Workspace cleanup skipped: ${e.message}"
                }
            }
        }
    }
}