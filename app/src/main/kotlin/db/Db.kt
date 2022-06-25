package db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

fun database(context: Context): Db {
    val driver = AndroidSqliteDriver(
        schema = Db.Schema,
        context = context,
        name = "thunder-v1.db",
    )

    return database(driver)
}

fun database(driver: SqlDriver): Db {
    return Db(driver)
}