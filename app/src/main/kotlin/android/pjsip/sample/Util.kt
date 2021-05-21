package android.pjsip.sample

import android.content.Context
import android.widget.Toast

fun Any.log(message: String) {
//    val kClass = this::class
    val tag = if (this::class.java.isAnonymousClass) {
        val result = this::class.java.name.split('.').let {
            it[it.lastIndex].split('$')
        }
        result.subList(0, result.lastIndex).joinToString(separator = "/") { it }
    } else {
        this::class.simpleName
    }
//    val simpleName = this::class.simpleName
//    val tag = if (simpleName.isNullOrEmpty()) this::class.java.name else simpleName
    println("[$tag|${this.hashCode()}] $message")
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
