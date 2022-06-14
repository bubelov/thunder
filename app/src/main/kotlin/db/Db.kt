package db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "thunder-v1.db",
    )

    return database(driver)
}

fun database(driver: SqlDriver): Database {
    return Database(driver)
}