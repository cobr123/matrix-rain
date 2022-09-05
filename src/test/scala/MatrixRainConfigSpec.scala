import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.{sequential, silent, timeout}

object MatrixRainConfigSpec extends ZIOSpecDefault {

  def spec =
    suite("MatrixRainConfigSpec")(
      test("--help") {
        for {
          _ <- Main.run.provide(ZLayer.fromZIO(ZIO.succeed(ZIOAppArgs(Chunk("--help")))))
          out <- TestConsole.output.map(_.mkString)
        } yield assertTrue(out.equals("""The famous Matrix rain effect of falling green characters as a cli command
                                        |Usage: matrix-rain [options]
                                        |
                                        |  -h, --help               Show this help message and exit
                                        |  -d, --direction <value>  {h, v}
                                        |                           Change direction of rain. h=horizontal, v=vertical
                                        |  -c, --color <value>      {green, red, blue, yellow, magenta, cyan, white}
                                        |                           Rain color. NOTE: droplet start is always white
                                        |  -k, --char-range <value>
                                        |                           {ascii, binary, braille, emoji, katakana}
                                        |                           Use rain characters from char-range
                                        |  -m, --mask-path <value>  Use the specified image to build a mask for the raindrops.
                                        |  -i, --invert-mask        Invert the mask specified with --mask-path.
                                        |  --offset-row <value>     Move the upper left corner of the mask down n rows.
                                        |  --offset-col <value>     Move the upper left corner of the mask right n columns.
                                        |  --font-ratio <value>     Ratio between character height over width in the terminal.
                                        |  --image-scale <value>    Scale image by ratio, default is 1.0
                                        |  --print-mask             Print mask and exit.
                                        |""".stripMargin))
      },
      test("--print-mask") {
        for {
          _ <- Main.run.provide(ZLayer.fromZIO(ZIO.succeed(ZIOAppArgs(Chunk("--print-mask")))))
          outErr <- TestConsole.outputErr.map(_.mkString)
        } yield assertTrue(outErr.equals("""Error: Missing option --mask-path
                                           |Try --help for more information.
                                           |""".stripMargin))
      },
      test("--mask-path morpheus --print-mask --image-scale") {
        Main.main(Array("--mask-path", "img/morpheus.jpeg", "--print-mask", "--image-scale", "0.3"))
        assertTrue(true)
      },
//      test("--mask-path morpheus --print-mask") {
//        Main.main(Array("--mask-path", "img/morpheus.jpeg", "--print-mask"))
//        assertTrue(true)
//      },
//      test("--mask-path fawkes --print-mask") {
//        Main.main(Array("--mask-path", "img/fawkes.jpeg", "--print-mask"))
//        assertTrue(true)
//      },
    ) @@ timeout(3.seconds) @@ silent

}
