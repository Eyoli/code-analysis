object Logger {

    private var enabled = true

    fun disable() {
        enabled = false
    }

    fun log(message: Any?) {
        if (enabled) println(message)
    }
}