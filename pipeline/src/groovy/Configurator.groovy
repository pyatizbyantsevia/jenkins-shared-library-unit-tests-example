void configureParams() {
    properties([
        parameters([
            string(
                name: 'JENKINS_URL',
                defaultValue: 'http://127.0.0.1:8080/'
            ),
            [name: 'isConfigured', $class: 'BooleanParameterDefinition', defaultValue: 'true']
        ])
    ])
}

return this
