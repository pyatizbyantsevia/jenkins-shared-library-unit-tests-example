void configureParams() {
    properties([
        parameters([
            [name: 'configured', $class: 'WHideParameterDefinition', defaultValue: 'yep', description: '']
        ])
    ])
}

return this
