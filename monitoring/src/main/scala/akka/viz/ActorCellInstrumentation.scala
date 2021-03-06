package akka.viz

import akka.actor.SupervisorStrategy.Directive
import akka.actor._
import akka.dispatch.MessageDispatcher
import akkaviz.config.Config
import akkaviz.events.EventSystem
import akkaviz.events.types._
import org.aspectj.lang.annotation._
import org.aspectj.lang.{JoinPoint, ProceedingJoinPoint}

@Aspect
class ActorCellInstrumentation {

  private[this] val internalSystemName = Config.internalSystemName

  @Pointcut(value = "execution (* akka.actor.ActorCell.receiveMessage(..)) && args(msg)", argNames = "msg")
  def receiveMessagePointcut(msg: Any): Unit = {}

  @Around(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp")
  def arroundMessage(jp: ProceedingJoinPoint, msg: Any, me: ActorCell): Any = {
    if (msg == ActorCellInstrumentation.RefreshInternalStateMsg) {
      EventSystem.report(CurrentActorState(me.self, me.actor))
    } else {
      jp.proceed()
    }
  }

  @Before(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def message(jp: JoinPoint, msg: Any, me: ActorCell) {
    if (me.system.name != internalSystemName) {
      Thread.sleep(EventSystem.receiveDelay.toMillis)
      EventSystem.report(MailboxStatus(me.self, me.mailbox.numberOfMessages))
    }
  }

  @After(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def afterMessage(jp: JoinPoint, msg: Any, me: ActorCell) {
    if (me.system.name != internalSystemName) {
      //FIXME: only if me.self is registered for tracking internal state
      EventSystem.report(CurrentActorState(me.self, me.actor))
    }
  }

  @Pointcut("execution(akka.actor.ActorCell.new(..)) && this(cell) && args(system, self, props, dispatcher)")
  def actorCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher): Unit = {}

  @After("actorCellCreation(cell, system, self, props, dispatcher)")
  def captureCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher): Unit = {
    if (cell.system.name != internalSystemName)
      EventSystem.report(Spawned(self))
  }

  @Pointcut("execution(* akka.actor.ActorCell.newActor()) && this(cell)")
  def actorCreation(cell: ActorCell): Unit = {}

  @AfterReturning(pointcut = "actorCreation(cell)", returning = "actor")
  def captureActorCreation(cell: ActorCell, actor: Actor): Unit = {
    if (cell.system.name != internalSystemName) {
      val self = cell.self
      EventSystem.report(Instantiated(self, actor))
      actor match {
        case fsm: FSM[_, _] =>
          //FIXME: unregister?
          fsm.onTransition {
            case (x, y) =>
              EventSystem.report(FSMTransition(self, x, fsm.stateData, y, fsm.nextStateData))
          }
        case _ => {}
      }

    }
  }

  @Pointcut("execution(* akka.actor.Actor.postStop()) && this(actor)")
  def actorTermination(actor: Actor): Unit = {}

  @After("actorTermination(actor)")
  def captureActorTermination(actor: Actor): Unit = {
    if (actor.context.system.name != internalSystemName) {
      EventSystem.report(Killed(actor.self))
    }
  }

  @Pointcut("execution(* akka.actor.SupervisorStrategy.logFailure(..)) && this(strategy) && args(context, child, cause, decision)")
  def handleFailure(strategy: SupervisorStrategy, context: ActorContext, child: ActorRef, cause: Throwable, decision: Directive): Unit = {}

  @After("handleFailure(strategy, context, child, cause, decision)")
  def captureHandleFailure(strategy: SupervisorStrategy, context: ActorContext, child: ActorRef, cause: Throwable, decision: Directive): Unit = {
    if (context.system.name != internalSystemName) {
      EventSystem.report(ActorFailure(child, cause, decision))
    }
  }

  @Pointcut("execution(* akka.actor.Actor.aroundReceive(..)) && this(actor) && args(receive, msg)")
  def aroundReceivePointcut(actor: Actor, receive: Actor.Receive, msg: Any): Unit = {}

  @Before("aroundReceivePointcut(actor, receive, msg)")
  def beforeAroundReceive(actor: Actor, receive: Actor.Receive, msg: Any): Unit = {
    if (actor.context.system.name != internalSystemName) {
      val canHandle = receive.isDefinedAt(msg)
      EventSystem.report(Received(actor.sender(), actor.self, msg, canHandle))
    }
  }

  @Pointcut("execution(* akka.actor.Actor.aroundPostRestart(..)) && this(actor)")
  def postRestartPointcut(actor: Actor): Unit = {}

  @Before("postRestartPointcut(actor)")
  def beforeAroundPostRestart(actor: Actor): Unit = {
    EventSystem.report(Restarted(actor.self))
  }
}

object ActorCellInstrumentation {

  case object RefreshInternalStateMsg

}