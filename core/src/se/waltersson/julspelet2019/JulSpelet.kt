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
  private val mainBackground = Texture(Gdx.files.internal("MainBackround.png"))
  private val mainTitle = Texture(Gdx.files.internal("MainTitle.png"))
  private val playBtnImg = Texture(Gdx.files.internal("ButtonPlay.png"))

  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }

  override fun render(delta: Float) {
    Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)

    camera.update()
    game.batch.projectionMatrix = camera.combined

    game.batch.use {
      it.draw(mainBackground, 0f, 0f)
      it.draw(mainTitle, 0f, 0f)
      it.draw(playBtnImg, 800 / 2 - 200 / 2f, 210f)
    }

    if (Gdx.input.isTouched) {
      game.setScreen<AdventureGameScreen>()
      dispose()
    }
  }
}

class AdventureGameScreen(julSpelet: JulSpelet) : KtxScreen {
  private val batch = julSpelet.batch
  private val font = julSpelet.font
  private val playerImage = Texture(Gdx.files.internal("JoshuaFrontStationary.png"))
  private val boxImage = Texture(Gdx.files.internal("BoxWood.png"))
  private val brickWallImage = Texture(Gdx.files.internal("BrickWall.png"))
  private val keyImage = Texture(Gdx.files.internal("GoldenKey.png"))
  private val doorImage = Texture(Gdx.files.internal("BrickWallLocked.png"))
  private val grass = Texture(Gdx.files.internal("Grass.png"))
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }
  private val grid = Grid(25, 15)
  private val avatar: GridOccupant = GridOccupant(x = 0,
      y = 0,
      image = playerImage)
  private val items = mutableListOf<GridOccupant>()
  private val hardLevel = """
    ...x...
    P..o...
    ...x.o.
    xxxxx.x
    .......
    x....x.
    ko...d.
    x......
  """.trimIndent()

  override fun show() {
    Gdx.input.inputProcessor = AvatarInputProcessor(avatar, items)
    initializeMap()
  }

  private fun initializeMap() {
    items.clear()
    hardLevel.lines().forEachIndexed { y, line ->
      line.forEachIndexed { x, square ->
        when (square) {
          'o' -> items.add(GridOccupant(x, y, boxImage))
          'x' -> items.add(GridOccupant(x, y, brickWallImage, false))
          'd' -> items.add(GridOccupant(x, y, doorImage, false))
          'k' -> items.add(GridOccupant(x, y, keyImage, false, consumable = true))
          'P' -> {
            avatar.x = x
            avatar.y = y
          }
        }
      }
    }
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
      for (gridX: Int in 0..grid.width) {
        for (gridY: Int in 0..grid.height) {
          batch.draw(grass, gridX * 32f, gridY * 32f)
        }
      }
      batch.draw(avatar.image, avatar.x * 32f, avatar.y * 32f)
      items.forEach {
        batch.draw(it.image, it.x * 32f, it.y * 32f)
      }
    }
  }

  data class Grid(val width: Int, val height: Int)

  data class Position(val x: Int, val y: Int) {
    fun overlaps(occupant: GridOccupant): Boolean {
      return occupant.x == this.x && occupant.y == this.y
    }
  }

  sealed class Movement(val x: Int, val y: Int) {
    class Left : Movement(-1, 0)
    class Right : Movement(1, 0)
    class Up : Movement(0, 1)
    class Down : Movement(0, -1)
    class Nothing : Movement(0, 0)
  }

  data class GridOccupant(
      var x: Int,
      var y: Int,
      val image: Texture,
      val canMove: Boolean = true,
      val consumable: Boolean = false)

  class AvatarInputProcessor(private val avatar: GridOccupant, private val items: MutableList<GridOccupant>) : KtxInputAdapter {
    override fun keyDown(keycode: Int): Boolean {
      val movement = when (keycode) {
        Input.Keys.LEFT -> Movement.Left()
        Input.Keys.RIGHT -> Movement.Right()
        Input.Keys.DOWN -> Movement.Down()
        Input.Keys.UP -> Movement.Up()
        else -> Movement.Nothing()
      }
      val actionPerformed =
          attemptConsume(avatar, movement)
              || attemptMove(avatar, movement)
      return true
    }

    private fun attemptConsume(occupant: GridOccupant, movement: Movement): Boolean {
      val wanted = occupant.positionAfterMoving(movement)
      if (wanted.outOfBounds()) {
        return false
      }

      items.forEach { otherOccupant ->
        if (wanted.overlaps(otherOccupant) && otherOccupant.consumable) {
          occupant.move(movement)
          // TODO occupant.consume(otherOccupant)
          items.remove(otherOccupant)
          return true
        }
      }
      return false
    }

    private fun attemptMove(occupant: GridOccupant, movement: Movement): Boolean {
      val wanted = occupant.positionAfterMoving(movement)
      if (!occupant.canMove || wanted.outOfBounds()) {
        return false
      }
      var canMove = true
      items.forEach { otherOccupant ->
        if (wanted.overlaps(otherOccupant)) {
          if (!attemptMove(otherOccupant, movement)) {
            canMove = false
          }
        }
      }
      if (canMove) {
        occupant.move(movement)
      }
      return canMove
    }
  }
}

private fun AdventureGameScreen.Position.outOfBounds(): Boolean {
  return x >= 25 || x < 0 || y >= 15 || y < 0
}

private fun AdventureGameScreen.GridOccupant.positionAfterMoving(movement: AdventureGameScreen.Movement): AdventureGameScreen.Position {
  return AdventureGameScreen.Position(this.x + movement.x, this.y + movement.y)
}

private fun AdventureGameScreen.GridOccupant.overlaps(occupant: AdventureGameScreen.GridOccupant): Boolean {
  return this.x == occupant.x && this.y == occupant.y
}

private fun AdventureGameScreen.GridOccupant.move(movement: AdventureGameScreen.Movement) {
  x += movement.x
  y += movement.y
}

