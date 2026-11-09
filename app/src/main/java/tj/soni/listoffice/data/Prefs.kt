package tj.soni.listoffice.data

import android.content.Context
import android.content.SharedPreferences

/** Танзимоти хурд (SharedPreferences). */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("list_office", Context.MODE_PRIVATE)

    fun getInt(key: String, def: Int): Int = sp.getInt(key, def)
    fun getFloat(key: String, def: Float): Float = sp.getFloat(key, def)
    fun getBool(key: String, def: Boolean): Boolean = sp.getBoolean(key, def)
    fun getString(key: String, def: String): String = sp.getString(key, def) ?: def

    fun edit(block: SharedPreferences.Editor.() -> Unit) {
        val e = sp.edit()
        e.block()
        e.apply()
    }
}
