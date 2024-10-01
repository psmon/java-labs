package actor

import akka.actor.typed.ActorRef

/** Models.. */
data class CounselorTask(
    val id: String,
    val client: String,
    var status: String = "대기중"
)

/** AgentActor가 처리할 수 있는 명령들 */
sealed class AgentCommand

data class AddCounselor(val name: String, val replyTo: ActorRef<Any>) : AgentCommand()
data class AssignTaskToCounselor(val counselorName: String, val task: CounselorTask, val replyTo: ActorRef<Any>) : AgentCommand()

data class AddGroup(val groupName: String, val skills: List<Skill>, val replyTo: ActorRef<AgentResponse>) : AgentCommand()
data class AddCounselorToGroup(val groupName: String, val counselorName: String, val replyTo: ActorRef<AgentResponse>) : AgentCommand()
data class AssignTaskToGroup(val groupName: String, val task: CounselorTask, val replyTo: ActorRef<AgentResponse>) : AgentCommand()


/** AgentActor가 반환할 수 있는 응답들 */
sealed class AgentResponse

data class CounselorAddedResponse(val name: String) : AgentResponse()
data class TaskAssignedResponse(val counselorName: String, val task: CounselorTask) : AgentResponse()
data class AgentErrorResponse(val message: String) : AgentResponse()

data class GroupAddedResponse(val groupName: String) : AgentResponse()
data class CounselorAddedToGroupResponse(val counselorName: String, val groupName: String) : AgentResponse()
data class TaskAssignedGroupResponse(val groupName: String, val task: CounselorTask) : AgentResponse()

/** 상담원 액터가 처리할 수 있는 명령들 */
sealed class CounselorCommand

data class GoOnline(val replyTo: ActorRef<Any>) : CounselorCommand()
data class GoOffline(val replyTo: ActorRef<Any>) : CounselorCommand()
data class AsignTask(val task: CounselorTask, val replyTo: ActorRef<Any>) : CounselorCommand()
data class StartTask(val task: CounselorTask, val replyTo: ActorRef<Any>) : CounselorCommand()
data class EndTask(val taskId: String, val replyTo: ActorRef<Any>) : CounselorCommand()
data class GetStatus(val replyTo: ActorRef<Any>) : CounselorCommand()

/** 상담원 액터가 반환할 수 있는 응답들 */
sealed class CounselorResponse

data class StatusResponse(val status: String) : CounselorResponse()
data class TaskStartedResponse(val task: CounselorTask) : CounselorResponse()
data class TaskEndedResponse(val taskId: String) : CounselorResponse()
data class ErrorResponse(val message: String) : CounselorResponse()