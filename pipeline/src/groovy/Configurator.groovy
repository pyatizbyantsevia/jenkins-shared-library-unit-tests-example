void configureParams() {
    properties([
        parameters([
            string(
                name: 'JENKINS_URL',
                defaultValue: 'http://192.168.3.33:32734/',
                description: 'Jenkisn URL'
            ),
            [name: 'isConfigured', $class: 'BooleanParameterDefinition', defaultValue: 'true']
        ])
    ])
}

return this
