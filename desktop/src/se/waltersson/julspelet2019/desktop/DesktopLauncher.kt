package se.waltersson.julspelet2019.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import se.waltersson.julspelet2019.JulSpelet

fun main(arg: Array<String>) {
  val config = LwjglApplicationConfiguration().apply {
    width = 800
  }
  LwjglApplication(JulSpelet(), config)
}
