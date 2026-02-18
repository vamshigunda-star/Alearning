package com.example.alearning.domain.usecases

class InterpretPushupCase {
    fun execute(count: Int, age: Int, gender: String): String {
return when {
    count >= 30 && age <18 -> "High Fitness Zone"
    count >= 20 && age <18 -> "Medium Fitness Zone"
else -> "Needs improvement"

}
    }
}