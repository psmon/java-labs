package actor.counselor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

/** AgentActor 클래스 */
class AgentActor private constructor(
    private val context: ActorContext<AgentCommand>,
    private val counselors: MutableMap<String, ActorRef<CounselorCommand>> = mutableMapOf(),
    private val distributor: Distributor = Distributor()
) : AbstractBehavior<AgentCommand>(context) {

    companion object {
        fun create(): Behavior<AgentCommand> {
            return Behaviors.setup { context -> AgentActor(context) }
        }
    }

    override fun createReceive(): Receive<AgentCommand> {
        return newReceiveBuilder()
            .onMessage(AddCounselor::class.java, this::onAddCounselor)
            .onMessage(AssignTaskToCounselor::class.java, this::onAssignTask)
            .build()
    }

    private fun onAddCounselor(command: AddCounselor): Behavior<AgentCommand> {
        if (counselors.containsKey(command.name)) {
            command.replyTo.tell(AgentErrorResponse("Counselor ${command.name} already exists."))
        } else {
            val counselorActor = context.spawn(CounselorActor.create(command.name), command.name)
            counselors[command.name] = counselorActor
            command.replyTo.tell(CounselorAddedResponse(command.name))
        }
        return this
    }

    private fun onAssignTask(command: AssignTaskToCounselor): Behavior<AgentCommand> {
        val counselorActor = counselors[command.counselorName]
        if (counselorActor != null) {
            counselorActor.tell(AsignTask(command.task, command.replyTo))
            command.replyTo.tell(TaskAssignedResponse(command.counselorName, command.task))
        } else {
            command.replyTo.tell(AgentErrorResponse("Counselor ${command.counselorName} not found."))
        }
        return this
    }
}