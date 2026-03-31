def call(String git_url, String git_branch, String cred_id = '') {
    // Validate branch name format
    if (!git_branch.matches('^[\\w.-]+$')) {
        error("Invalid branch name format")
    }
    
    // Validate URL format
    if (!git_url.matches('^https?://[\\w.-]+(/[\\w.-]+)*(\\.git)?$')) {
        error("Invalid git URL format")
    }
    
     try {
        timeout(time: 5, unit: 'MINUTES') {
            def gitConfig = [
                branch: git_branch,
                url: git_url,
                changelog: true,
                poll: false
            ]
            
            if (cred_id) {
                gitConfig.credentialsId = cred_id
            }
            
            git(gitConfig)
        }
     } catch (Exception e) {
        def errorMsg = "Git checkout failed:\n" +
                      "Branch: ${git_branch}\n" +
                      "URL: ${git_url}\n" +
                      "Error: ${e.message}"
        error(errorMsg)
     }
}
