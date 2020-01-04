package se.waltersson.julspelet2019

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.min

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

private val hardLevel = """
    ...x..kx
    S..o...x
    ...x.o.x
    xxxxx^x.
    x.....xx
    ko....DD
    x.....xx
    xxxxxxx.
  """.trimIndent()
private val powerLineLevel = """
    ..............
    ..............
    .....0.P......
    .....1........
    .....2........
    S....3........
    ..............
  """.trimIndent()

class AdventureGameScreen(julSpelet: JulSpelet) : KtxScreen {
  private val batch = julSpelet.batch
  private val font = BitmapFont()
  private val playerImage = Texture(Gdx.files.internal("JoshuaFrontStationary.png"))
  private val boxImage = Texture(Gdx.files.internal("BoxWood.png"))
  private val brickWallImage = Texture(Gdx.files.internal("BrickWall.png"))
  private val keyImage = Texture(Gdx.files.internal("GoldenKey.png"))
  private val doorImage = Texture(Gdx.files.internal("BrickWallLocked.png"))
  private val powerlineWallRightOffImage = Texture(Gdx.files.internal("PowerlineWallRightOff.png"))
  private val powerlineWallLeftOffImage = Texture(Gdx.files.internal("PowerlineWallLeftOff.png"))
  private val powerBlockImage = Texture(Gdx.files.internal("PowerBlock.png"))
  private val grass = Texture(Gdx.files.internal("Grass.png"))
  private val rockyGrass = Texture(Gdx.files.internal("RockyGrass.png"))
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }
  private val grid = Grid(25, 15)
  private val avatar: GridOccupant.Player = GridOccupant.Player(
      x = 0,
      y = 0,
      image = playerImage)
  private val items = mutableListOf<GridOccupant>()
  private val textReceiver = TextReceiver()

  override fun show() {
    Gdx.input.inputProcessor = AvatarInputProcessor(avatar, items, textReceiver, this)
    initializeMap(hardLevel)
  }

  private fun initializeMap(level: String) {
    items.clear()
    level.lines().forEachIndexed { y, line ->
      line.forEachIndexed { x, square ->
        when (square) {
          'o' -> items.add(GridOccupant.Movable(x, y, boxImage))
          'P' -> items.add(GridOccupant.Movable(x, y, powerBlockImage))
          '0' -> items.add(GridOccupant.Movable(x, y, powerlineWallRightOffImage))
          '1' -> items.add(GridOccupant.Movable(x, y, powerlineWallLeftOffImage))
          '2' -> items.add(GridOccupant.Movable(x, y, powerlineWallRightOffImage, 180f))
          '3' -> items.add(GridOccupant.Movable(x, y, powerlineWallLeftOffImage, 180f))
          'x' -> items.add(GridOccupant.Immovable(x, y, brickWallImage))
          'D' -> items.add(GridOccupant.Door(x, y, doorImage))
          'k' -> items.add(GridOccupant.Consumable(x, y, keyImage))
          '^' -> items.add(GridOccupant.Clutter(x, y, rockyGrass))
          'S' -> {
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
    batch.use(camera) { batch ->
      renderGround(batch)
      renderItems(batch)
      renderPlayer(batch)
      renderTexts(batch, delta)
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
      it.addToBatch(batch)
    }
  }

  private fun renderTexts(batch: SpriteBatch, delta: Float) {
    val completeTexts = mutableListOf<TextReceiver.TextAnimation>()
    textReceiver.texts.forEach {
      it.update(delta)
      val color: Color = font.color
      font.setColor(color.r, color.g, color.b, it.alpha)
      font.draw(batch, it.text, 100f, 100f)
      if (it.complete) {
        completeTexts.add(it)
      }
    }
    textReceiver.texts.removeAll(completeTexts)
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
    open fun addToBatch(batch: SpriteBatch) {
      batch.draw(image, x * 32f, y * 32f)
    }

    class Consumable(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Movable(x: Int, y: Int, image: Texture, private val rotation: Float = 0f) : GridOccupant(x, y, image, canMove = true) {
      private val sprite: Sprite = Sprite(image)
      override fun addToBatch(batch: SpriteBatch) {
        sprite.setPosition(x * 32f, y * 32f)
        sprite.rotation = rotation
        sprite.draw(batch)
      }
    }
    class Immovable(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Clutter(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)
    class Player(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image, canMove = true) {
      var keys: Int = 0
    }

    class Door(x: Int, y: Int, image: Texture) : GridOccupant(x, y, image)

  }

  class TextReceiver {
    val texts = mutableListOf<TextAnimation>()

    fun showText(text: String) {
      texts.add(TextAnimation(text))
    }

    class TextAnimation(val text: String, private val lifeTime: Float = 2f) {
      private var elapsed: Float = 0f
      private var easeTime: Float = 0.2f
      val complete: Boolean
        get() = elapsed >= lifeTime
      private val easeIn: Interpolation = Interpolation.circleIn
      private val easeOut: Interpolation = Interpolation.circleOut

      fun update(delta: Float) {
        elapsed += delta
      }

      val alpha: Float
        get() {
          if (elapsed <= easeTime) {
            return easeIn.apply(min(1f, elapsed / easeTime))
          } else if (elapsed > (lifeTime - easeTime)) {
            return easeOut.apply(min(1f, (lifeTime - elapsed) / easeTime))
          }
          return 1f
        }
    }
  }

  class AvatarInputProcessor(private val avatar: GridOccupant.Player, private val items: MutableList<GridOccupant>,
                             private val textReceiver: TextReceiver,
                             private val game: AdventureGameScreen) : KtxInputAdapter {

    override fun keyDown(keycode: Int): Boolean {
      val movement = when (keycode) {
        Input.Keys.LEFT -> Movement.Left()
        Input.Keys.RIGHT -> Movement.Right()
        Input.Keys.DOWN -> Movement.Down()
        Input.Keys.UP -> Movement.Up()
        else -> Movement.Nothing()
      }
      when (keycode) {
        Input.Keys.NUM_1 -> game.initializeMap(hardLevel)
        Input.Keys.NUM_2 -> game.initializeMap(powerLineLevel)
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
      val consumedItems = mutableListOf<GridOccupant>()
      items.forEach { otherOccupant ->
        if (wanted.overlaps(otherOccupant)) {
          if (otherOccupant is GridOccupant.Consumable) {
            consumedItems.add(otherOccupant)
            avatar.keys += 1
            textReceiver.showText("Picked up a key, you now have ${avatar.keys} key${if (avatar.keys > 1) "s" else ""}")
          } else if (otherOccupant is GridOccupant.Door && avatar.keys > 0) {
            consumedItems.add(otherOccupant)
            avatar.keys -= 1
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

private fun AdventureGameScreen.GridOccupant.move(movement: AdventureGameScreen.Movement) {
  x += movement.x
  y += movement.y
}

