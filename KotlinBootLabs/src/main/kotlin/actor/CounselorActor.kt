import actor.*
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

/** 상담원 액터 클래스 */
class CounselorActor private constructor(
    private val context: ActorContext<CounselorCommand>,
    private val name: String,
    private var onlineStatus: Boolean = false,
    private val tasks: MutableMap<String, CounselorTask> = mutableMapOf()
) : AbstractBehavior<CounselorCommand>(context) {

    companion object {
        fun create(name: String): Behavior<CounselorCommand> {
            return Behaviors.setup { context -> CounselorActor(context, name) }
        }
    }

    init {
        context.log.info("$name 상담원 액터가 생성되었습니다.")
    }

    override fun createReceive(): Receive<CounselorCommand> {
        return newReceiveBuilder()
            .onMessage(GoOnline::class.java, this::onGoOnline)
            .onMessage(GoOffline::class.java, this::onGoOffline)
            .onMessage(AsignTask::class.java, this::onGoAsignTask)
            .onMessage(StartTask::class.java, this::onStartTask)
            .onMessage(EndTask::class.java, this::onEndTask)
            .onMessage(GetStatus::class.java, this::onGetStatus)
            .build()
    }

    private fun onGoOnline(command: GoOnline): Behavior<CounselorCommand> {
        onlineStatus = true
        command.replyTo.tell(StatusResponse("$name 상담원이 온라인 상태입니다."))
        return this
    }

    private fun onGoOffline(command: GoOffline): Behavior<CounselorCommand> {
        onlineStatus = false
        command.replyTo.tell(StatusResponse("$name 상담원이 오프라인 상태입니다."))
        return this
    }

    private fun onGoAsignTask(command: AsignTask): Behavior<CounselorCommand> {
        if (!onlineStatus) {
            command.replyTo.tell(ErrorResponse("$name 상담원이 온라인 상태가 아닙니다."))
            return this
        }
        tasks[command.task.id] = command.task.apply { status = "상담대기중" }
        command.replyTo.tell(TaskStartedResponse(command.task))
        return this
    }

    private fun onStartTask(command: StartTask): Behavior<CounselorCommand> {
        if (!onlineStatus) {
            command.replyTo.tell(ErrorResponse("$name 상담원이 온라인 상태가 아닙니다."))
            return this
        }
        tasks[command.task.id] = command.task.apply { status = "상담진행중" }
        command.replyTo.tell(TaskStartedResponse(command.task))
        return this
    }

    private fun onEndTask(command: EndTask): Behavior<CounselorCommand> {
        val task = tasks.remove(command.taskId)
        if (task != null) {
            task.status = "상담종료"
            command.replyTo.tell(TaskEndedResponse(command.taskId))
        } else {
            command.replyTo.tell(ErrorResponse("Task ${command.taskId}가 존재하지 않습니다."))
        }
        return this
    }

    private fun onGetStatus(command: GetStatus): Behavior<CounselorCommand> {
        val status = if (onlineStatus) "Online" else "Offline"
        command.replyTo.tell(StatusResponse("$name 상담원은 $status 상태이며, 진행 중인 Task 수는 ${tasks.size}개입니다."))
        return this
    }
}