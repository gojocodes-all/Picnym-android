package ng.name.gojodev.picnym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ng.name.gojodev.picnym.ui.PicnymApp
import ng.name.gojodev.picnym.ui.theme.PicnymTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PicnymTheme {
                PicnymApp()
            }
        }
    }
}
