package se.waltersson.julspelet2019

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.use

class JulSpelet : KtxGame<Screen>() {
  lateinit var font: BitmapFont
    private set
  lateinit var batch: SpriteBatch
    private set

  override fun create() {
    batch = SpriteBatch()
    font = BitmapFont()
    addScreen(DropGameScreen(this))
    addScreen(MainMenuScreen(this))
    addScreen(AdventureGameScreen(this))
    setScreen<MainMenuScreen>()
  }

  override fun dispose() {
    font.dispose()
  }
}

class MainMenuScreen(private val game: JulSpelet) : KtxScreen {
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }

  override fun render(delta: Float) {
    Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.update()
    game.batch.projectionMatrix = camera.combined

    game.batch.use {
      game.font.draw(game.batch, "Welcome to Drop!!! ", 100f, 150f)
      game.font.draw(game.batch, "Tap anywhere to begin!", 100f, 100f)
    }

    if (Gdx.input.isTouched) {
      game.setScreen<AdventureGameScreen>()
      dispose()
    }
  }
}
class AdventureGameScreen(julSpelet: JulSpelet): KtxScreen {
  private val batch = julSpelet.batch
  private val font = julSpelet.font
  private val playerImage = Texture(Gdx.files.internal("player.png"))
  private val poopImage = Texture(Gdx.files.internal("poop.png"))
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }
  private val grid = Grid(25, 15)
  private val avatar: GridOccupant = GridOccupant(x = 5,
    y = 5,
    width = 32f,
    height = 32f)
  private val items = listOf(
      GridOccupant(10, 10, 32f, 32f),
      GridOccupant(15, 8, 32f, 32f)
      )

  override fun show() {
    Gdx.input.inputProcessor = AvatarInputProcessor(avatar, items)
  }

  override fun render(delta: Float) {
    Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    if (avatar.x < 0) avatar.x = 0
    if (avatar.x >= grid.width) avatar.x = grid.width - 1
    if (avatar.y < 0) avatar.y = 0
    if (avatar.y >= grid.height) avatar.y = grid.height - 1

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.use { batch ->
      batch.draw(playerImage, avatar.x * 32f, avatar.y * 32f)
      items.forEach {
        batch.draw(poopImage, it.x * 32f, it.y * 32f)
      }
    }
  }

  data class Grid(val width: Int, val height: Int)

  sealed class Movement(val x: Int, val y: Int) {
    class Left: Movement(-1, 0)
    class Right: Movement(1, 0)
    class Up: Movement(0, 1)
    class Down: Movement(0, -1)
    class Nothing: Movement(0, 0)
  }

  data class GridOccupant(var x: Int, var y: Int, val width: Float, val height: Float)

  class AvatarInputProcessor(private val avatar: GridOccupant, private val items: List<GridOccupant>): KtxInputAdapter {
    override fun keyDown(keycode: Int): Boolean {
      val movement = when(keycode) {
        Input.Keys.LEFT -> Movement.Left()
        Input.Keys.RIGHT -> Movement.Right()
        Input.Keys.DOWN -> Movement.Down()
        Input.Keys.UP -> Movement.Up()
        else -> Movement.Nothing()
      }
      avatar.move(movement)
      items.forEach {
        if (avatar.overlaps(it)) {
          it.move(movement)
        }
      }
      return true
    }
  }
}

private fun AdventureGameScreen.GridOccupant.overlaps(occupant: AdventureGameScreen.GridOccupant): Boolean {
  return this.x == occupant.x && this.y == occupant.y
}

private fun AdventureGameScreen.GridOccupant.move(movement: AdventureGameScreen.Movement) {
  x += movement.x
  y += movement.y
}

