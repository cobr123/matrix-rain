import org.jline.terminal.Terminal

import scala.collection.mutable.ArrayBuffer

final case class MatrixRain(terminal: Terminal, matrixRainConfig: MatrixRainConfig) {

  private var transpose = matrixRainConfig.direction.equals("h")
  private var color = matrixRainConfig.color
  private val maxSpeed = 20
  private val colDroplets = new ArrayBuffer[List[Droplet]]()
  // Simple string stream buffer + stdout flush at once
  private val outBuffer = new ArrayBuffer[String]()

  private def rand(start: Int, end: Int): Int = {
    (start + Math.floor(Math.random() * (end - start))).toInt
  }

  private def randCharString(start: Int, end: Int): String = {
    val ch = (start + Math.floor(Math.random() * (end - start))).toChar
    String.valueOf(ch)
  }

  private def generateChars(len: Int, charRange: String): Array[String] = {
    // by default charRange == ascii
    if (charRange.equalsIgnoreCase("ascii")) {
      Array.fill[String](len)(randCharString(0x21, 0x7E))
    } else if (charRange.equalsIgnoreCase("binary")) {
      Array.fill[String](len)(randCharString(0x30, 0x32))
    } else if (charRange.equalsIgnoreCase("braille")) {
      Array.fill[String](len)(randCharString(0x2840, 0x28ff))
    } else if (charRange.equalsIgnoreCase("katakana")) {
      Array.fill[String](len)(randCharString(0x30a0, 0x30ff))
    } else if (charRange.equalsIgnoreCase("emoji")) {
      // emojis are two character widths, so use a prefix
      val emojiPrefix = String.valueOf(0xd83d.toChar)
      Array.fill[String](len)(emojiPrefix + randCharString(0xde01, 0xde4a))
    } else if (charRange.equalsIgnoreCase("lil-guys")) {
      // Force horizontal direction
      if (!transpose) {
        transpose = true
        color = "white"
        start()
      }
      Array.fill[String](len)("  ~~o ")
    } else ???
  }

  private def makeDroplet(col: Int, row: Int = rand(0, numRows)): Droplet = {
    new Droplet(
      alive = 0,
      curCol = col,
      curRow = row,
      height = rand(numRows / 2, numRows),
      speed = rand(1, maxSpeed),
      chars = generateChars(numRows, matrixRainConfig.charRange),
    )
  }

  private var numCols = 0
  private var numRows = 0

  def resizeDroplets(): Unit = {
    numCols = terminal.getWidth
    numRows = terminal.getHeight

    // transpose for direction
    if (transpose) {
      val tmpNumCols = numCols
      numCols = numRows
      numRows = tmpNumCols
    }

    // Create droplets per column
    // add/remove droplets to match column size
    if (numCols > colDroplets.length) {
      var col = colDroplets.length
      while (col < numCols) {
        // make two droplets per row that start in random positions
        colDroplets += List(makeDroplet(col), makeDroplet(col))
        col += 1
      }
    } else {
      colDroplets.sliceInPlace(0, numCols)
    }
  }

  private def writeAt(row: Long, col: Long, str: String, color: String): Unit = {
    // Only output if in viewport
    if (row >= 0 && row < this.numRows && col >= 0 && col < this.numCols) {
      val pos = if (transpose) {
        Ansi.cursorPos(col, row)
      } else {
        Ansi.cursorPos(row, col)
      }
      write(s"$pos$color$str")
    }
  }

  def renderFrame(): Unit = {
    val ansiColor = AnsiColor.fgGreen

    for (droplets <- colDroplets) {
      for (droplet <- droplets) {
        val (curRow, curCol, height) = (droplet.curRow, droplet.curCol, droplet.height)
        droplet.alive += 1

        if (droplet.alive % droplet.speed == 0) {
          writeAt(curRow - 1, curCol, droplet.getCharOrEmpty(curRow - 1), ansiColor)
          writeAt(curRow, curCol, droplet.getCharOrEmpty(curRow), AnsiColor.fgWhite)
          writeAt(curRow - height, curCol, " ", "")
          droplet.curRow += 1
        }

        if (curRow - height > numRows) {
          // reset droplet
          val d = makeDroplet(curCol, 0)
          droplet.reset(d)
        }
      }
    }

    flush()
  }

  private def write(str: String): Unit = {
    outBuffer += str
  }

  private def flush(): Unit = {
    terminal.writer.println(outBuffer.mkString)
    terminal.writer().flush()
    outBuffer.clear()
  }

  def start(): MatrixRain = {
    // clear terminal and use alt buffer
    terminal.enterRawMode()
    write(Ansi.useAltBuffer)
    write(Ansi.cursorInvisible)
    write(AnsiColor.bgBlack)
    write(AnsiColor.fgBlack)
    write(Ansi.clearScreen)
    flush()
    resizeDroplets()
    this
  }

  def stop(): Unit = {
    write(Ansi.cursorVisible)
    write(Ansi.clearScreen)
    write(Ansi.cursorHome)
    write(Ansi.useNormalBuffer)
    flush()
  }
}
