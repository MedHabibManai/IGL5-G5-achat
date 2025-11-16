// jenkins/stages/sendEmailNotification.groovy
// Email notification stage for Jenkins pipeline

def call(Map config = [:]) {
    def defaultConfig = [
        subject: "Build ${env.BUILD_NUMBER} - ${env.JOB_NAME}",
        body: "",
        to: env.EMAIL_RECIPIENTS ?: "",
        attachLog: true,
        attachBuildLog: true
    ]
    
    config = defaultConfig + config
    
    // Only send email if recipients are configured
    echo "Email notification - Recipients: '${config.to}'"
    if (!config.to || config.to.trim().isEmpty()) {
        echo "Email notification skipped: No recipients configured (set EMAIL_RECIPIENTS environment variable)"
        echo "Current EMAIL_RECIPIENTS env var: '${env.EMAIL_RECIPIENTS ?: 'NOT SET'}'"
        return
    }
    
    echo "Attempting to send email to: ${config.to}"
    
    try {
        // Get build status
        def buildStatus = currentBuild.currentResult ?: 'UNKNOWN'
        def statusColor = buildStatus == 'SUCCESS' ? 'green' : buildStatus == 'FAILURE' ? 'red' : 'yellow'
        def statusIcon = buildStatus == 'SUCCESS' ? '✅' : buildStatus == 'FAILURE' ? '❌' : '⚠️'
        
        // Get deployment info from Terraform if available
        def applicationUrl = ""
        def publicIp = ""
        def instanceId = ""
        
        try {
            dir("${env.TERRAFORM_DIR}") {
                applicationUrl = sh(
                    script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                publicIp = sh(
                    script: 'terraform output -raw public_ip 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                instanceId = sh(
                    script: 'terraform output -raw instance_id 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
            }
        } catch (Exception e) {
            echo "Could not retrieve Terraform outputs: ${e.message}"
        }
        
        // Build email subject
        def emailSubject = config.subject ?: "${statusIcon} ${buildStatus}: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
        
        // Build email body
        def emailBody = """
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 800px; margin: 0 auto; padding: 20px; }
        .header { background-color: #${statusColor == 'green' ? '4CAF50' : statusColor == 'red' ? 'f44336' : 'ff9800'}; color: white; padding: 20px; border-radius: 5px 5px 0 0; }
        .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }
        .section { margin-bottom: 20px; }
        .section h3 { color: #${statusColor == 'green' ? '4CAF50' : statusColor == 'red' ? 'f44336' : 'ff9800'}; border-bottom: 2px solid #${statusColor == 'green' ? '4CAF50' : statusColor == 'red' ? 'f44336' : 'ff9800'}; padding-bottom: 5px; }
        .info-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        .info-table td { padding: 8px; border-bottom: 1px solid #ddd; }
        .info-table td:first-child { font-weight: bold; width: 200px; }
        .link { color: #2196F3; text-decoration: none; }
        .link:hover { text-decoration: underline; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>${statusIcon} Build ${buildStatus}</h1>
            <p><strong>Job:</strong> ${env.JOB_NAME}</p>
            <p><strong>Build Number:</strong> #${env.BUILD_NUMBER}</p>
        </div>
        
        <div class="content">
            <div class="section">
                <h3>📊 Build Information</h3>
                <table class="info-table">
                    <tr>
                        <td>Status:</td>
                        <td><strong style="color: ${statusColor};">${buildStatus}</strong></td>
                    </tr>
                    <tr>
                        <td>Duration:</td>
                        <td>${currentBuild.durationString ?: 'N/A'}</td>
                    </tr>
                    <tr>
                        <td>Deployment Mode:</td>
                        <td>${params.DEPLOYMENT_MODE ?: 'NORMAL'}</td>
                    </tr>
                    <tr>
                        <td>Branch:</td>
                        <td>${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'}</td>
                    </tr>
                    <tr>
                        <td>Commit:</td>
                        <td>${env.GIT_COMMIT ?: 'N/A'}</td>
                    </tr>
                </table>
            </div>
            
            ${applicationUrl ? """
            <div class="section">
                <h3>🌐 Deployment Information</h3>
                <table class="info-table">
                    <tr>
                        <td>Application URL:</td>
                        <td><a href="${applicationUrl}" class="link">${applicationUrl}</a></td>
                    </tr>
                    <tr>
                        <td>Health Check:</td>
                        <td><a href="${applicationUrl.replace('/SpringMVC', '/SpringMVC/actuator/health')}" class="link">Health Status</a></td>
                    </tr>
                    <tr>
                        <td>Swagger UI:</td>
                        <td><a href="${applicationUrl.replace('/SpringMVC', '/SpringMVC/swagger-ui/index.html')}" class="link">API Documentation</a></td>
                    </tr>
                    ${publicIp ? """
                    <tr>
                        <td>Public IP:</td>
                        <td>${publicIp}</td>
                    </tr>
                    """ : ""}
                    ${instanceId ? """
                    <tr>
                        <td>Instance ID:</td>
                        <td>${instanceId}</td>
                    </tr>
                    """ : ""}
                </table>
            </div>
            """ : ""}
            
            <div class="section">
                <h3>🔗 Quick Links</h3>
                <p>
                    <a href="${env.BUILD_URL}" class="link">View Build Console</a> | 
                    <a href="${env.BUILD_URL}console" class="link">Full Console Output</a> | 
                    <a href="${env.JOB_URL}" class="link">Job Dashboard</a>
                </p>
            </div>
            
            ${config.body ? """
            <div class="section">
                <h3>📝 Additional Information</h3>
                <p>${config.body}</p>
            </div>
            """ : ""}
        </div>
        
        <div class="footer">
            <p>This is an automated email from Jenkins CI/CD Pipeline</p>
            <p>Build triggered at: ${new Date()}</p>
        </div>
    </div>
</body>
</html>
        """
        
        // Send email using Email Extension Plugin
        // Note: emailext doesn't throw exceptions, it logs errors internally
        // We can't directly check if it succeeded, but we'll add warnings
        echo "========================================="
        echo "Attempting to send email notification..."
        echo "Recipients: ${config.to}"
        echo "Subject: ${emailSubject}"
        echo "========================================="
        
        try {
            // Use mail() instead of emailext() because:
            // - Extended E-mail Notification requires OAuth2 (complex setup)
            // - Default E-mail Notification uses SMTP Authentication (already working)
            // - Test email works, so default email config is correct
            mail(
                to: config.to,
                subject: emailSubject,
                body: emailBody,
                mimeType: 'text/html'
            )
            
            echo "========================================="
            echo "Email send command executed"
            echo "WARNING: Check console output above for connection errors!"
            echo "If you see 'Connection error' or 'Failed after second try',"
            echo "the email was NOT sent successfully."
            echo "========================================="
            echo ""
            echo "Troubleshooting steps if email not received:"
            echo "1. Check console output for 'Connection error' messages"
            echo "2. Verify SMTP settings in Jenkins: Manage Jenkins → Configure System"
            echo "3. Test SMTP configuration using 'Test configuration' button"
            echo "4. Check spam/junk folder"
            echo "5. Verify EMAIL_RECIPIENTS is set correctly: '${env.EMAIL_RECIPIENTS ?: 'NOT SET'}'"
            echo ""
            
        } catch (Exception e) {
            echo "========================================="
            echo "EXCEPTION while calling emailext:"
            echo "Error: ${e.message}"
            echo "========================================="
            throw e
        }
        
    } catch (Exception e) {
        echo "========================================="
        echo "FAILED to send email notification!"
        echo "Error: ${e.message}"
        echo "Error class: ${e.getClass().getName()}"
        echo "Stack trace:"
        e.getStackTrace().each { line ->
            echo "  ${line}"
        }
        echo "========================================="
        // Don't fail the build if email fails
    }
}

return this

