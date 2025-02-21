object ContinueExample:

  def main(args: Array[String]): Unit =
    val numbers = List(1, 2, 3, 4, 5, 6)

    println("Printing only odd numbers:")
    for number <- numbers do
      if number % 2 == 0 then  // If the number is even...
        () // ...skip to the next iteration (pseudo-continue)
      else
        println(number)  // Otherwise, print the number

    println("\nUsing a filter instead (more idiomatic):")
    val oddNumbers = numbers.filter(_ % 2 != 0)
    oddNumbers.foreach(println)
