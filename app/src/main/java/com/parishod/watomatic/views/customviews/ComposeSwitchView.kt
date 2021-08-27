package com.parishod.watomatic.views.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme

//REF: https://compose.academy/blog/migrating_to_compose_-_composeview
class ComposeSwitchView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {


    // Create a State for the title text so any changes can be observed and reflected automatically
    // in our Composable Text.
    var titleText by mutableStateOf("")
    var onClick by mutableStateOf({})
    var onChecked by mutableStateOf({})
    var checked by mutableStateOf(false)
    var switchEnabled by mutableStateOf(true)

    @ExperimentalMaterialApi
    @Composable
    override fun Content() {
        AppCompatTheme{
            MaterialSwitchComponent(onClick)
        }
    }

    // We represent a Composable function by annotating it with the @Composable annotation. Composable
// functions can only be called from within the scope of other composable functions. We should
// think of composable functions to be similar to lego blocks - each composable function is in turn
// built up of smaller composable functions.
    @ExperimentalMaterialApi
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun MaterialSwitchComponent(
        onClick: () -> Unit,
    ) {
        // Reacting to state changes is the core behavior of Compose. You will notice a couple new
        // keywords that are compose related - remember & mutableStateOf.remember{} is a helper
        // composable that calculates the value passed to it only during the first composition. It then
        // returns the same value for every subsequent composition. Next, you can think of
        // mutableStateOf as an observable value where updates to this variable will redraw all
        // the composable functions that access it. We don't need to explicitly subscribe at all. Any
        // composable that reads its value will be recomposed any time the value
        // changes. This ensures that only the composables that depend on this will be redraw while the
        // rest remain unchanged. This ensures efficiency and is a performance optimization. It
        // is inspired from existing frameworks like React.
        //var checked by remember { mutableStateOf(false) }

        // Card composable is a predefined composable that is meant to represent the card surface as
        // specified by the Material Design specification. We also configure it to have rounded
        // corners and apply a modifier.

        // You can think of Modifiers as implementations of the decorators pattern that are used to
        // modify the composable that its applied to. In the example below, we add a padding of
        // 8dp to the Card composable. In addition, we configure it out occupy the entire available
        // width using the Modifier.fillMaxWidth() modifier.
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            border = BorderStroke(1.5.dp,
                        when{
                            !switchEnabled ->  Color(192, 192, 192)
                            checked -> MaterialTheme.colors.secondaryVariant
                            else -> Color(255, 69, 0)
                        }),
            shape = RoundedCornerShape(5.dp),
            onClick = onClick,
            backgroundColor = Color(255, 255, 255)
        ) {
            // Row is a composable that places its children in a horizontal sequence. You can think of it
            // similar to a LinearLayout with the horizontal orientation. In addition, we pass a modifier
            // to the Row composable.
            Row(modifier = Modifier.padding(16.dp), Arrangement.SpaceBetween) {
                // The Text composable is pre-defined by the Compose UI library; you can use this
                // composable to render text on the screen
                Text(text = titleText, modifier = Modifier.padding(8.dp))
                // A pre-defined composable that's capable of rendering a switch. It honors the Material
                // Design specification.
                Switch(
                    modifier = Modifier
                    .padding(8.dp),
                    enabled = switchEnabled,
                    checked = checked,
                    onCheckedChange = {
                    checked = !checked
                    onChecked.invoke()
                })
            }
        }
    }
}