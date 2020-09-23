package migration

import zio.{ExitCode, URIO, ZIO}

import scala.util.Random
import scala.xml.Elem

object GenXML extends zio.App {

  def xml(): Elem = {
    <hello>
      <this>is some xml</this>
      <it is="valid">
        <and>Has children</and>
      </it>
      <nested>
        <data>
          {System.currentTimeMillis()}
          <description>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</description>
          <meh>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?</meh>
        </data>
      </nested>
    </hello>
  }

  val invalid =
    """    <hello>
      |      <this>is some xml</this>
      |      <it is="valid">
      |        <and>Has children</and>
      |      </it>
      |      <nested>
      |        <data>
      |        </data
      |        <description>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</description>
      |        <meh>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?</meh>
      |      </nested>
      |    </hello>""".stripMargin

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    import eie.io._
    val dir = "target-zip".asPath.mkDirs()
    def next() = Random.nextInt(100000000).toString.padTo(9, '0')
    val nr = (0 to 20).count { i =>
      dir.resolve(s"AB_CD_${i.toString.reverse.padTo(3, '0').reverse}_${next()}.xml").text = xml().toString
      true
    }
//    dir.resolve(s"file${nr}.xml").text = invalid
    ZIO.succeed(ExitCode.success)
  }
}
