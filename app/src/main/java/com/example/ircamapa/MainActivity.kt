package com.example.ircamapa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ircamapa.ui.IrcaMapScreen
import com.example.ircamapa.ui.IrcaViewModel
import com.example.ircamapa.ui.theme.IrcaMapaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IrcaMapaTheme {
                val viewModel: IrcaViewModel = viewModel()
                IrcaMapScreen(viewModel = viewModel)
            }
        }
    }
}
