package com.eternal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterNameScreen(phone: String, onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    // Background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA)) // Soft cyan to teal
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient) // Apply gradient background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title Text
        Text(
            text = "Enter Your Name",
            fontSize = 24.sp,
            color = Color(0xFF007BB5), // Deep blue
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input Field with soft rounded corners
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name", color = Color(0xFF007BB5)) }, // Deep blue label
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), // Rounded corners
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color(0xFF007BB5), // Deep blue
                focusedBorderColor = Color(0xFF80DEEA), // Bright teal
                unfocusedBorderColor = Color(0xFFB2EBF2), // Light cyan
                focusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button with rounded corners and vibrant colors
        Button(
            onClick = { onNameEntered(name) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5AD2F4), // Bright cyan
                contentColor = Color.White // White text
            )
        ) {
            Text("Submit", fontSize = 16.sp)
        }
    }
}
