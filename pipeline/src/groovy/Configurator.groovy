void configureParams() {
    properties([
        parameters([
            [ name: 'isConfigured', $class: 'BooleanParameterDefinition', defaultValue: 'true']
        ])
    ])
}

return this
