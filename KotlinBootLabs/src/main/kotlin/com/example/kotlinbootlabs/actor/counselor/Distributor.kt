package com.example.kotlinbootlabs.actor.counselor

import org.apache.pekko.actor.typed.ActorRef

data class Skill(val name: String, val priority: Int)

class Distributor {
    private val counselorGroups: MutableMap<String, MutableList<ActorRef<CounselorActor>>> = mutableMapOf()
    private val groupSkills: MutableMap<String, List<Skill>> = mutableMapOf()
    private val roundRobinIndex: MutableMap<String, Int> = mutableMapOf()

    fun addGroup(groupName: String, skills: List<Skill>) {
        groupSkills[groupName] = skills.sortedBy { it.priority }
        counselorGroups[groupName] = mutableListOf()
        roundRobinIndex[groupName] = 0
    }

    fun addCounselorToGroup(groupName: String, counselor: ActorRef<CounselorActor>) {
        counselorGroups[groupName]?.add(counselor)
    }

    fun getNextCounselor(groupName: String): ActorRef<CounselorActor>? {
        val counselors = counselorGroups[groupName] ?: return null
        if (counselors.isEmpty()) return null

        val index = roundRobinIndex[groupName]!!
        val counselor = counselors[index]

        roundRobinIndex[groupName] = (index + 1) % counselors.size
        return counselor
    }

    fun getGroupSkills(groupName: String): List<Skill>? {
        return groupSkills[groupName]
    }
}