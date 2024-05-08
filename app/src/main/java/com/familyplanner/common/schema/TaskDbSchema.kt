package com.familyplanner.common.schema

class TaskDbSchema {
    companion object {
        const val TASK_TABLE = "task"
        const val TITLE = "title"
        const val DEADLINE = "deadline"
        const val IS_CONTINUOUS = "isContinuous"
        const val START_TIME = "startTime"
        const val FINISH_TIME = "finishTime"
        const val REPEAT_TYPE = "repeatType"
        const val N_DAYS = "nDays"
        const val DAYS_OF_WEEK = "daysOfWeek"
        const val REPEAT_START = "repeatStart"
        const val IMPORTANCE = "importance"
        const val LOCATION = "location"
        const val CREATED_BY = "createdBy"
        const val FAMILY_ID = "familyId"
        const val PREVIOUS_COMPLETION_DATE = "previousCompletionDate"
        const val LAST_COMPLETION_DATE = "lastCompletionDate"
        const val PARENT_ID = "parentId"
    }
}