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
  private val rockyGrass = Texture(Gdx.files.internal("RockyGrass.png"))
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }
  private val grid = Grid(25, 15)
  private val avatar: GridOccupant = GridOccupant.Player(
      x = 0,
      y = 0,
      image = playerImage)
  private val items = mutableListOf<GridOccupant>()
  private val hardLevel = """
    ...x...
    P..o...
    ...x.o.
    xxxxx^x
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
          'o' -> items.add(GridOccupant.Movable(x, y, boxImage))
          'x' -> items.add(GridOccupant.Immovable(x, y, brickWallImage))
          'd' -> items.add(GridOccupant.Immovable(x, y, doorImage))
          'k' -> items.add(GridOccupant.Consumable(x, y, keyImage))
          '^' -> items.add(GridOccupant.Clutter(x, y, rockyGrass))
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

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.use { batch ->
      renderGround(batch)
      renderItems(batch)
      renderPlayer(batch)
    }
  }

  private fun renderGround(batch: SpriteBatch) {
    for (gridX: Int in 0..grid.width) {
      for (gridY: Int in 0..grid.height) {
        batch.draw(grass, gridX * 32f, gridY * 32f)
      }
    }
  }

  private fun renderPlayer(batch: SpriteBatch) {
    batch.draw(avatar.image, avatar.x * 32f, avatar.y * 32f)
  }

  private fun renderItems(batch: SpriteBatch) {
    items.forEach {
      batch.draw(it.image, it.x * 32f, it.y * 32f)
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

  sealed class GridOccupant(
      var x: Int,
      var y: Int,
      val image: Texture,
      val canMove: Boolean = false) {

    class Consumable(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Movable(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image, canMove = true)
    class Immovable(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Clutter(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Player(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image, canMove = true)
  }

  class AvatarInputProcessor(private val avatar: GridOccupant, private val items: MutableList<GridOccupant>) : KtxInputAdapter {
    override fun keyDown(keycode: Int): Boolean {
      val movement = when (keycode) {
        Input.Keys.LEFT -> Movement.Left()
        Input.Keys.RIGHT -> Movement.Right()
        Input.Keys.DOWN -> Movement.Down()
        Input.Keys.UP -> Movement.Up()
        else -> Movement.Nothing()
      }
      attemptMove(avatar, movement)
      return true
    }

    private fun attemptMove(occupant: GridOccupant, movement: Movement): Boolean {
      val wanted = occupant.positionAfterMoving(movement)
      if (!occupant.canMove || wanted.outOfBounds()) {
        return false
      }
      var canMove = true
      val consumedItems = mutableListOf<GridOccupant.Consumable>()
      items.forEach { otherOccupant ->
        if (wanted.overlaps(otherOccupant)) {
          if (otherOccupant is GridOccupant.Consumable) {
            consumedItems.add(otherOccupant)
          } else if (!occupant.canOverlap(otherOccupant) && !attemptMove(otherOccupant, movement)) {
            canMove = false
          }
        }
      }
      if (canMove) {
        occupant.move(movement)
        items.removeAll(consumedItems)
      }
      return canMove
    }
  }
}

private fun AdventureGameScreen.GridOccupant.canOverlap(otherOccupant: AdventureGameScreen.GridOccupant): Boolean {
  return this is AdventureGameScreen.GridOccupant.Player
      && (otherOccupant is AdventureGameScreen.GridOccupant.Clutter
      || otherOccupant is AdventureGameScreen.GridOccupant.Consumable)
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

