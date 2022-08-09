import org.jline.terminal.{Terminal, TerminalBuilder}
import zio._


object Main extends ZIOAppDefault {

  override def run = {
    program
      .provideLayer(matrixRainLayer)
  }

  def program: ZIO[MatrixRain, Nothing, Unit] =
    for {
      matrixRain <- ZIO.service[MatrixRain]
      _ <- ZIO.succeed(matrixRain.renderFrame()).repeat(Schedule.spaced((1000 / 60).millis)) // 60FPS
    } yield ()

  def configAndTerminalLayer: ZLayer[Scope with ZIOAppArgs, Any, Terminal with MatrixRainConfig] = ZLayer.fromZIO(makeTerminal) ++ MatrixRainConfig.live

  def matrixRainLayer: ZLayer[Scope with ZIOAppArgs, Any, MatrixRain] = configAndTerminalLayer >>> ZLayer.fromZIO(makeMatrixRain)
  
  private def makeTerminal: ZIO[Scope, Throwable, Terminal] =
    ZIO.acquireRelease(
      ZIO.attempt(TerminalBuilder.builder()
        .jna(true)
        .system(true)
        .build())
    )(t => ZIO.succeed(t.close()))

  private def makeMatrixRain: ZIO[Scope with Terminal with MatrixRainConfig, Throwable, MatrixRain] =
    for {
      terminal <- ZIO.service[Terminal]
      matrixRainConfig <- ZIO.service[MatrixRainConfig]
      matrixRain <- ZIO.acquireRelease(ZIO.attempt(MatrixRain(terminal, matrixRainConfig).start()))(mr => ZIO.succeed(mr.stop()))
    } yield matrixRain
}