package ru.itdo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.itdo.app.core.AppContainer
import ru.itdo.app.ui.nav.AppNav
import ru.itdo.app.ui.theme.ItdoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = AppContainer(applicationContext)
        setContent {
            ItdoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(container)
                }
            }
        }
    }
}
