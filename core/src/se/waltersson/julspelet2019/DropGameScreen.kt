package se.waltersson.julspelet2019

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.random.Random

class DropGameScreen(julSpelet: JulSpelet) : KtxScreen {
  private val batch = julSpelet.batch
  private val font = julSpelet.font
  private val dropImage = Texture(Gdx.files.internal("droplet.png"))
  private val fireImage = Texture(Gdx.files.internal("fire.png"))
  private val bucketImage = Texture(Gdx.files.internal("bucket.png"))
  private val dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"))
  private val rainMusic: Sound = Gdx.audio.newSound(Gdx.files.internal("rain.mp3"))
  private val camera = OrthographicCamera(800f, 480f).apply {
    setToOrtho(false)
  }
  private val raindrops = Array<Rectangle>()
  private val firedrops = Array<Firedrop>()
  private var lastDropTime: Long = 0
  private var score = 0

  private val bucket = Rectangle().apply {
    x = 800f / 2f - 64f / 2f
    y = 20f
    width = 64f
    height = 64f
  }

  override fun show() {
    rainMusic.loop()
  }

  override fun render(delta: Float) {
    Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    camera.update()
    batch.projectionMatrix = camera.combined
    raindrops.forEachIndexed { index, raindrop ->
      raindrop.y -= 200 * delta
      if (raindrop.y + 64 < 0) raindrops.removeIndex(index)
      if (raindrop.overlaps(bucket)) {
        dropSound.play()
        score++
        raindrops.removeIndex(index)
      }
    }
    firedrops.forEachIndexed { index, firedrop ->
      firedrop.boundingBox.y -= firedrop.speed * delta
      if (firedrop.boundingBox.y + 64 < 0) firedrops.removeIndex(index)
      if (firedrop.boundingBox.overlaps(bucket)) {
        dropSound.play()
        score -= 5
        firedrops.removeIndex(index)
      }
    }

    batch.use {
      raindrops.forEach { raindrop ->
        batch.draw(dropImage, raindrop.x, raindrop.y)
      }
      firedrops.forEach { firedrop ->
        batch.draw(fireImage, firedrop.boundingBox.x, firedrop.boundingBox.y)
      }
      it.draw(bucketImage, bucket.x, bucket.y)
      font.draw(it, "Score: $score", 100f, 100f)
    }
    if (Gdx.input.isTouched) {
      val touchPos = Vector3()
      touchPos.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
      camera.unproject(touchPos)
      bucket.x = touchPos.x - 64 / 2
    }
    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) bucket.x -= 300 * delta
    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) bucket.x += 300 * delta
    if (bucket.x < 0) bucket.x = 0f
    if (bucket.x > 800 - 64) bucket.x = 800f - 64
    if (TimeUtils.nanoTime() - lastDropTime > 1000000000) spawnDrop()
  }

  private fun spawnDrop() {
    val drop = Rectangle().apply {
      x = Random.nextInt(800 - 64).toFloat()
      y = 480f
      width = 64f
      height = 64f
    }
    if (Random.nextInt(10) == 0) {
      firedrops.add(Firedrop(drop, Random.nextInt(150, 400).toFloat()))
    } else {
      raindrops.add(drop)
    }
    lastDropTime = TimeUtils.nanoTime()
  }

  override fun dispose() {
    batch.dispose()
    dropImage.dispose()
    bucketImage.dispose()
    dropSound.dispose()
  }

  data class Firedrop(val boundingBox: Rectangle, val speed: Float)
}