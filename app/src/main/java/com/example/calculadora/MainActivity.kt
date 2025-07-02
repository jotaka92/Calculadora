package com.example.calculadora

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculadora.ui.theme.CalculadoraTheme
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculatorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color = Color.White,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isWide: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(
                if (isWide) Modifier.aspectRatio(2f / 1f)
                else Modifier.aspectRatio(1f / 1f)
            )
            .padding(4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontSize = when (text) {
                "⌫", "%" -> 20.sp
                else -> 28.sp
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CalculatorApp(modifier: Modifier = Modifier) {
    var currentInputExpression by remember { mutableStateOf("0") } // Esta será la pantalla principal de entrada
    var previousCalculationDisplay by remember { mutableStateOf("") } // Para mostrar "1+2=" arriba

    // Estados internos para la lógica de cálculo, no directamente para la UI principal
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var currentOperator by remember { mutableStateOf<String?>(null) }
    // Para identificar si la última acción fue un operador, para formatear currentInputExpression
    var lastActionWasOperator by remember { mutableStateOf(false) }
    var justCalculated by remember { mutableStateOf(false) }


    // Formateador para evitar la notación científica en números grandes y controlar decimales
    val numberFormatter = DecimalFormat("0.#######").apply {
        maximumFractionDigits = 7
        isGroupingUsed = false // No usar comas para miles
    }

    fun formatNumberForDisplay(number: Double): String {
        if (number.isNaN()) return "Error"
        if (number.isInfinite()) return "Infinito"
        val formatted = numberFormatter.format(number)
        return if (formatted.length > 15) "Error: Desborde" else formatted
    }


    fun performCalculation(op1: Double, op2: Double, operator: String): Double {
        return when (operator) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "×" -> op1 * op2
            "÷" -> if (op2 == 0.0) Double.NaN else op1 / op2
            else -> op2
        }
    }

    // Extrae el último número de la expresión para operar con él
    fun extractLastNumber(expression: String): Double? {
        // Encuentra el último operador
        val lastOpIndex = expression.indexOfLast { it == '+' || it == '-' || it == '×' || it == '÷' }
        val numberStr = if (lastOpIndex == -1 || (lastOpIndex == 0 && expression.startsWith("-"))) {
            expression // Es el primer número (o un negativo al inicio)
        } else {
            expression.substring(lastOpIndex + 1).trim()
        }
        return numberStr.toDoubleOrNull()
    }


    fun handleNumberClick(number: String) {
        if (justCalculated) {
            currentInputExpression = number
            previousCalculationDisplay = "" // Limpiar la expresión anterior
            firstOperand = null
            currentOperator = null
            justCalculated = false
            lastActionWasOperator = false
            return
        }

        if (currentInputExpression == "0" && number != ".") {
            currentInputExpression = number
        } else if (lastActionWasOperator) {
            currentInputExpression += " $number" // Añadir espacio si la última acción fue un operador
        } else {
            if (!(currentInputExpression.endsWith(".") && number == ".")) { // Evitar múltiples puntos si el último fue un punto
                if(currentInputExpression.split(" ").lastOrNull()?.contains(".") == true && number == "."){
                    // No añadir otro punto si el número actual ya tiene uno
                } else if (currentInputExpression.length < 30) { // Límite de longitud general
                    currentInputExpression += number
                }
            }
        }
        lastActionWasOperator = false
    }

    fun handleOperatorClick(operator: String) {
        justCalculated = false
        val currentNumberInExpression = extractLastNumber(currentInputExpression)

        if (currentNumberInExpression != null) {
            if (firstOperand == null) { // Primer operando
                firstOperand = currentNumberInExpression
            } else if (currentOperator != null) { // Operación encadenada
                val result = performCalculation(firstOperand!!, currentNumberInExpression, currentOperator!!)
                firstOperand = result // El resultado es el nuevo primer operando
                // No actualizamos currentInputExpression con el resultado intermedio aquí
                // porque queremos que la expresión completa siga construyéndose.
            }
            // Si la última acción fue un operador, reemplazarlo
            if (lastActionWasOperator && currentInputExpression.length > 2) {
                currentInputExpression = currentInputExpression.dropLast(2).trimEnd() + " $operator"
            } else {
                currentInputExpression += " $operator"
            }
            currentOperator = operator
            lastActionWasOperator = true

        } else if (operator == "-" && (currentInputExpression == "0" || lastActionWasOperator)) {
            // Manejar inicio de número negativo o después de otro operador
            if (currentInputExpression == "0") currentInputExpression = "-"
            else currentInputExpression += " -" // ej. 5 * -
            lastActionWasOperator = false // Permitir escribir números después del '-'
        }
    }

    fun handleEqualsClick() {
        val lastNumber = extractLastNumber(currentInputExpression)

        if (firstOperand != null && currentOperator != null && lastNumber != null) {
            val result = performCalculation(firstOperand!!, lastNumber, currentOperator!!)
            previousCalculationDisplay = "$currentInputExpression ="
            currentInputExpression = formatNumberForDisplay(result)

            firstOperand = null // Reset para la siguiente operación completa
            currentOperator = null
            justCalculated = true
            lastActionWasOperator = false
        } else if (firstOperand == null && currentOperator == null && lastNumber != null) {
            // Caso: solo se ingresó un número y se presionó =
            previousCalculationDisplay = "${formatNumberForDisplay(lastNumber)} ="
            currentInputExpression = formatNumberForDisplay(lastNumber)
            justCalculated = true
        }
    }

    fun handleClearClick() {
        currentInputExpression = "0"
        previousCalculationDisplay = ""
        firstOperand = null
        currentOperator = null
        lastActionWasOperator = false
        justCalculated = false
    }

    fun handleBackspaceClick() {
        if (justCalculated) {
            handleClearClick()
            return
        }
        if (currentInputExpression.isNotEmpty()) {
            currentInputExpression = currentInputExpression.dropLast(1).trimEnd()
            if (currentInputExpression.isEmpty()) {
                currentInputExpression = "0"
            }
            // Re-evaluar si la última acción es un operador
            lastActionWasOperator = currentInputExpression.endsWith(" +") ||
                    currentInputExpression.endsWith(" -") ||
                    currentInputExpression.endsWith(" ×") ||
                    currentInputExpression.endsWith(" ÷")

            // Podrías necesitar recalcular el firstOperand y currentOperator si se borra un operador
            // Esto es complejo y se omite por simplicidad aquí
        }
    }

    fun handlePercentageClick() {
        val currentNumber = extractLastNumber(currentInputExpression)
        if (currentNumber != null) {
            val percentageResult = currentNumber / 100.0
            val formattedPercentage = formatNumberForDisplay(percentageResult)

            // Reemplazar el último número con su versión en porcentaje
            if (firstOperand != null && currentOperator != null) { // Si es parte de una expresión más grande
                val lastOpIndex = currentInputExpression.indexOfLast { it == '+' || it == '-' || it == '×' || it == '÷' }
                if (lastOpIndex != -1) {
                    currentInputExpression = currentInputExpression.substring(0, lastOpIndex + 1) + " $formattedPercentage"
                } else { // Si es el primer número
                    currentInputExpression = formattedPercentage
                }
            } else { // Si es un único número
                currentInputExpression = formattedPercentage
            }
            // Opcionalmente, podrías querer que esto se evalúe inmediatamente si hay una operación pendiente.
            // Por ahora, solo actualiza la expresión.
            justCalculated = false // El % no finaliza el cálculo como '='
        }
    }


    val numberButtonColor = Color(0xFF424242)
    val operatorButtonColor = Color(0xFFFFA000)
    val specialButtonColor = Color(0xFF616161)
    val equalsButtonColor = Color(0xFFE91E63)
    val operatorContentColor = Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Pantalla SUPERIOR: Muestra la expresión calculada anteriormente o está vacía
        Text(
            text = previousCalculationDisplay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            fontSize = 24.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 2
        )

        // Pantalla PRINCIPAL / INFERIOR: Muestra la entrada actual en tiempo real o el resultado
        Text(
            text = currentInputExpression,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 0.dp, start = 8.dp, end = 8.dp),
            fontSize = 48.sp, // Un poco más pequeño para acomodar expresiones más largas
            color = Color.White,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 2 // Permitir 2 líneas por si la expresión se vuelve muy larga
        )

        val buttonModifier = Modifier.weight(1f)
        val buttonSpacing = 10.dp

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
            CalculatorButton("C", specialButtonColor, Color.White, buttonModifier, onClick = { handleClearClick() })
            CalculatorButton("⌫", specialButtonColor, Color.White, buttonModifier, onClick = { handleBackspaceClick() })
            CalculatorButton("%", specialButtonColor, Color.White, buttonModifier, onClick = { handlePercentageClick() })
            CalculatorButton("÷", operatorButtonColor, operatorContentColor, buttonModifier, onClick = { handleOperatorClick("÷") })
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
            CalculatorButton("7", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("7") })
            CalculatorButton("8", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("8") })
            CalculatorButton("9", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("9") })
            CalculatorButton("×", operatorButtonColor, operatorContentColor, buttonModifier, onClick = { handleOperatorClick("×") })
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
            CalculatorButton("4", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("4") })
            CalculatorButton("5", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("5") })
            CalculatorButton("6", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("6") })
            CalculatorButton("-", operatorButtonColor, operatorContentColor, buttonModifier, onClick = { handleOperatorClick("-") })
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
            CalculatorButton("1", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("1") })
            CalculatorButton("2", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("2") })
            CalculatorButton("3", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick("3") })
            CalculatorButton("+", operatorButtonColor, operatorContentColor, buttonModifier, onClick = { handleOperatorClick("+") })
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
            CalculatorButton("0", numberButtonColor, Color.White, buttonModifier.weight(2f), isWide = true, onClick = { handleNumberClick("0") })
            CalculatorButton(".", numberButtonColor, Color.White, buttonModifier, onClick = { handleNumberClick(".") })
            CalculatorButton("=", equalsButtonColor, Color.White, buttonModifier, onClick = { handleEqualsClick() })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    CalculadoraTheme {
        CalculatorApp()
    }
}