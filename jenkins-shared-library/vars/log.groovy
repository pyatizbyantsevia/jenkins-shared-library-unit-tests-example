import mainpackage.LogLevel

def call(String message, LogLevel logLevel) {
    ansiColor('xterm') {        
        switch (logLevel) {
            case LogLevel.INFO:
                echo "\033[0;32m\033[1m[INFO]" + message + "\033[0m"
                break
            case LogLevel.ERROR:
                echo "\033[0;31m\033[1m[ERROR]" + message + "\033[0m"
                break
            case LogLevel.WARNING:
                echo "\033[0;33m\033[1m[WARNING]" + message + "\033[0m"
                break
            case LogLevel.NOTICE:
                echo "\033[0;34m\033[1m[NOTICE]" + message + "\033[0m"
        }
    }
}
