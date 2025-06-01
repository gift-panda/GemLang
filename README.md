How to Use:
===============

Download the Interpreter.jar and GemGui.jar and keep in the SAME directory.
Use jdk 9+ to execute the jar.

OR

Run the Interpreter.jar from the terminal, passing the path to a .gem file for execution.

Declarations
===================
    
    var name = value;

Declares a variable and assigns a value.

also decomposed into

    var name;
    name = value;

Operators
====================

Gem operator heirarchy is as follows:

Primary operators include "this", "super", "(", ")"

    this.x; -> Refers to the instance that is calling 
    super.x; -> Refers to the super class
    this.x * (super.x + this.x); -> Groups expressions

Call operators include "(" params* ")", "[" index "]", "[" start ":" end "]", "." dot operator

    function(params*); -> Calls functions
    list[1]; -> Retrives value from list
    list[1:4]; -> Slices a portion of the list
    instance.property; -> Retrives a property of an instance

Unary operators include "+" and "-"

    +x; -> No-op 
    -y; -> Negates a number

Multiplicative operators include "*", "/", "\\", "%"

    x * y; -> Multiplication
    x / y; -> Division
    x \ y; -> Integer division
    x % y; -> Remainder after division

Additive operands "+" and "-"

    x + y; -> Addition
    x - y; -> Substraction

Comparision operators include ">", ">=", "<", "<="

    x > y; -> Greater than
    x >= y; -> Greater then equal to
    x < y; -> Lesser than
    x <= y; -> Lesser than equal to

Equality operators include "==" and "!="

    x == y; -> Equals
    x != y; -> Not equals

Boolean operatos include "and" and "or"

    x and y; -> Boolean and, has higer precedence
    x or y; -> Boolean or, has lower precedence

Operator Overloads
=====================

Gem translates: value1 **symbol** value2 as value1.**symbol**(value2).

All binary operators can be overloaded using the 'operator' keyword in the defination of the method.

	class ClassName {
        operator SYMBOL(parameter) {
            // implementation using this and parameter
            return some_value;
        }
    }

**operator** keyord signals an overload.
**SYMBOL** is the operator to overload.

Examples:

    class Example {
        init(value) {
            this.value = value;
        }

        operator +(other) {
            // Access the instance's value and other operand's value
            return this.value + other.value;
        }

        operator *(other) {
            return this.value * other.value;
        }

        operator >(other) {
            return this.value > other.value;
        }
    }

    var first = Example(10);
    var second = Example(20);
    println(first + second); -> 30
    println(first * second); -> 200
    println(first > second); -> false


Conditionals
=====================

Gem has only one type of branching control structure.

If statements:

    if(condition){
        body
    }
    else if(condition){
        body
    }
    .
    .
    .
    else{
        body
    }
     
Refer to operators section for boolean operations on contitions.

Loops
=================

    for(var i = start; i <= end; i = i + 1){
        body
    }
    while(condition){
        body
    }

These are Indeprendent blocks with their own scopes

    {
        body
    } 


Functions
==================

For functions:

    func name(param1, param2, ...){
        body
    }

Functions are overloaded by arity

    func name(param1){
        body 
    }
    func name(param1, param2){
        body 
    }

Both are dispatched at runtime to their correct function calls.

Functions can be binded to any name.

    func name(params){
        body
    }
    var bind = name;
    bind(params);


Imports
=====================

Gem allows two types of imports:
1. Internals imports (suffix of gem. )
2. User imports 

Internals imports require the syntax "gem.FILENAME"

    import gem.Math;
    import gem.Util;
    import gem.String;
    import gem.Number;
    import gem.Boolean;
    import gem.List;
    import gem.RuntimeError;

Internal imports point directly two standard libs associated with the language.

User imports use the syntax "FILENAME"

    import test;
    import sample;

These are used to import user generated Gem files.

The files you want to import must be present in the same directory as your main file.


Returns
===================

Returns use the syntax:
    
    return expression;

You are forced to return an expression(that yeild values) and not statements.


Classes
====================

Classes use the syntax:

    class className : superClass(){ 
        // className is the child class. superClass is the parent class. 

        var var1 = value; //Treated as public & static fields
        var #var = value; //Treated as private & static fields. 

        static staticFunc(params){ 
            //Treated as static function.
            //Can access private and static fields here
            
            body
        }

        instFunc(params){ 
            //Treated as instance functions.
            
            this.var2 = value; //Treated as public & instance fields.
            this.#var3 = value; //Treated as private & instance fields.

            //Can access private and static fields here.
            //Can access private and instance fields here.
        
            body
        }

        init(params){ 
            //Constructor of the class.
            //Default constructor with no parms created implicitly when explicitly not defined.
            //Can be private #init() to stop creation of instances.
            
            body
        }

        toString(){ 
            //Implicitly called by the interpreter when 1. Printing a instance. 2. Added instances to strings. 
            //Default created for all class implicitly is explicitly not defined.
            
            return string;
        }
    }


    className.var1; //Accessing static fields
    className.staticFunct(params) //Accessing static methods.

    this.var2 or inst.var2; //Accessing instance fields.
    className(params) //Creating instance of a class. <init>s can also overloaded and dispatched at runtime.


Errors
=====================

Types of Errors:
1. RuntimeError -> Thrown by the interpreter at runtime.
2. SyntaxError -> Thrown by the lexer, parser and resolver at compiletime.

RuntimeErrors:

    var errorInstance = RuntimeError(String); 
    //The base error class
    //Extend your class using RuntimeError to be throw

    throw errorInstance; //To throw a error
    //or
    throw SomeError("message"); //To throw your error
    //or
    throw RuntimeError("message"); //To throw the default RuntimeError.

To catch a RuntimError

    try{
        body
    }
    catch(IDENTIFIER){ //IDENTIFIER is the captured the error.
        body
    }
    finally{
        body
    }



__InBuilt Errors:__
1. RuntimeError
2. IllegalArgumentError
3. IllegalArgumentsError
4. IndexOutOfBoundsError
5. NumberFormatError
6. BooleanFormatError

__Properties of RuntimeErrors:__
1. getMessage() -> Returns the message passed
2. getStackTrace() -> Returns the stack trace associated with the object
3. toString() -> Returns the message shown when thrown


Value Types
=====================

__There exist:__
1. Number
2. Boolean
3. String
4. List

All are wrapped at time of creation implicitly.

Lists and Strings are special as they support indexing and slicing.

    var a = "Some String";
    a[num] -> Gives the character present at index num.
    a[start:end] -> Gives a substring of the string from index start to end.


    var b = [1, 2, 3, 4, 5];
    b[num] -> Gives the value present at index num.
    b[start:end] -> Gives a list containing the values from index start to end.

Wrapper Classes
======================

Gem doesnt contain any primitive type exposed to the user.

All inbuilt types are wrapped into wrapper classes.

__Including:__
1. Number
2. Boolean
3. String
4. List

__Number function:__

    var number;

1. init(Number) -> Provided for support. Creates a new Number using existing Number object

    number = Number(20); -> 20

2. toString() -> Provided for support. Returns the number.

    number.toString(); -> "20"

__Boolean functions:__

    var bool;

1. init(Boolean) -> Provided for support. Creates a new Boolean using existing Boolean object.

    bool = Boolean(true); -> true

2. toString() -> Provided for support. Returns the boolean.

    bool.toString(); -> "true"

__String functions:__

    var str;
    var num;
    var bool;

1. init(String) -> Provided for support. Creates a new String using an existing string.

    str = String("Hello, world!"); -> "Hello, world!"
    num = String("20"); -> "20"
    bool = String("true"); -> "true"

2. parse() -> Parses the string and returns a Boolean, Number, or a String if conversion not possible.

        str.parser(); -> "Hello, world!"
        num.parse(); -> 20
        bool.parse(); -> true
    
3. parseBoolean() -> Forces a Boolean to be parsed. Throws BooleanFormatError if conversion not possible.

        bool.parseBoolean(); -> true;

4. parseNumber() -> Forces a Number to be parsed. Throws NumberFormatError if coneversion not possible.

        num.parseNumber(); -> 20

5. isDigit() -> Returns a Boolean by checking if the first character of the given string is a digit.

        num.isDigit(); -> true

6. isLetter() -> Returns a Boolean by checking if the first character of the given string is a letter.

        str.isLetter(); -> true
        bool.isLetter(); -> true

7. isAlphanumeric() -> Returns a Boolean by checking if the first character of the given string is either a digit or a letter.

        str.isAlphanumeric(); -> true
        num.isAlphanumeric(); -> true
        bool.isAlphanumeric(); -> true
        "!".isAlphanumeric(); -> false

8. toUpperCase() -> Returns a String by changing all the lowercase letters into uppercase.

        str.toUpperCase(); -> "HELLO, WORLD!"

9. toLowerCase() -> Returns a String by changing all the uppercase letters into lowercase.

        str.toLowerCase(); -> "hello, world!"

10. trim() -> Returns a String by stripping off the leading/trailing white spaces.

        "   Hello, world!    ".trim(); -> "Hello, world!"

11. startsWith(prefix) -> Returns a Boolean by checking if the given string starts with the given prefix.

        str.startsWith("Hello"); -> true
        str.startsWith("hello"); -> false

12. endsWith(suffix) -> Returns a Boolean by checking if the given string ends with the given suffix.

        str.endsWith("world"); -> false
        str.endsWith("world!"); -> true

13. replace(old, new) -> Returns a String by replacing every occurrence of the old substring with the new substring in the string.

        "foo bar foo".replace("foo", "baz"); -> "baz bar baz"

14. split(delimiter) -> Returns a List by separating the string into chunks using the delimiter string.

        "a,b,c".split(","); -> ["a", "b", "c"]

15. indexOf(substring) -> Returns a Number by finding the index of a substring in the given string. Returns -1 if the substring is not present.

        "hello".indexOf("e"); -> 1
        "hello".indexOf("z"); -> -1

16. contains(substring) -> Returns a Boolean by checking if a substring is present in the given string.

        "hello".contains("ll"); -> true
        "hello".contains("zz"); -> false

17. toString() -> Provided for support. Returns the given string.

        "hello".toString(); -> "hello"

18. length() -> Returns the length of the string.

        "hello".length(); -> 5

__List functions:__
1. init(list) -> Provided for support. Creates a new List object using an existing List.

    var a = [1, 2, 3];
    var b = init(a); -> [1, 2, 3]

2. length() -> Returns the length of the list.

        [1, 2, 3].length(); -> 3

3. append(value) -> Adds a value to the end of the list.

        var a = [1, 2];
        a.append(3); -> [1, 2, 3]

4. insert(value, index) -> Adds a value to the list at index and shifts the remaining list to the right.

        var a = [1, 3];
        a.insert(2, 1); -> [1, 2, 3]

5. removeAt(index) -> Removes the value in the list at index and shifts the remaining list to the left.

        var a = [1, 2, 3];
        a.removeAt(1); -> [1, 3]

6. get(index) -> Provided for support. Returns the value in the list at index.

        [10, 20, 30].get(1); -> 20

7. set(value, index) -> Provided for support. Sets the value at index to given value.

        var a = [1, 2, 3];
        a.set(9, 1); -> [1, 9, 3]

8. contains(value) -> Returns a Boolean by checking if the list contains a value.

        [1, 2, 3].contains(2); -> true
        [1, 2, 3].contains(5); -> false

9. indexOf(value) -> Returns the index of a value in the list.

        [10, 20, 30].indexOf(20); -> 1
        [10, 20, 30].indexOf(40); -> -1

10. clear() -> Clears the list.

        var a = [1, 2, 3];
        a.clear(); -> []

11. isEmpty() -> Returns a Boolean by checking if the list is empty.

        [].isEmpty(); -> true
        [1].isEmpty(); -> false

12. toString() -> Returns the string representation of the list.

        [1, 2, 3].toString(); -> "[1, 2, 3]"

13. clone() -> Shallow copies the list to another list.

        var a = [1, 2, 3];
        var b = a.clone(); -> [1, 2, 3]

14. reverse() -> Reverses the list.

        var a = [1, 2, 3];
        a.reverse(); -> [3, 2, 1]

15. slice(start, end) -> Returns a chunk of the list from start to end position.

        [1, 2, 3, 4].slice(1, 3); -> [2, 3]

16. sort() -> Sorts the list. Uses quickSort by default.

        var a = [3, 1, 2];
        a.sort(); -> [1, 2, 3]

17. quickSort() -> Performs QuickSort on the list.

        var a = [3, 1, 2];
        a.quickSort(); -> [1, 2, 3]

18. swap(i, j) -> Swaps the position of two values by index.

        var a = [1, 2, 3];
        a.swap(0, 2); -> [3, 2, 1]

__Math functions:__
1. static sqrt(num) -> Returns the square root of a number.

        Math.sqrt(25); -> 5

2. static ln(num) -> Returns the natural log of a number.

        Math.ln(1); -> 0

3. static exp(num) -> Returns e raised to the power of num.

        Math.exp(1); -> 2.718...

4. static pow(a, b) -> Returns a raised to the power of b.

        Math.pow(2, 3); -> 8

5. static round(num, decimals) -> Rounds a number to a given number of decimals.

        Math.round(3.14159, 2); -> 3.14

6. static round(num) -> Rounds a number to the nearest integer.

        Math.round(2.7); -> 3

7. static abs(x) -> Returns the absolute value of a number.

        Math.abs(-10); -> 10

8. static ceil(x) -> Returns the nearest integer greater than a number.

        Math.ceil(2.1); -> 3

9. static floor(x) -> Returns the nearest integer lesser than a number.

        Math.floor(2.9); -> 2

10. static max(a, b) -> Returns the greater number between a and b.

        Math.max(4, 7); -> 7

11. static min(a, b) -> Returns the lesser number between a and b.

        Math.min(4, 7); -> 4

12. static sign(x) -> 1 for positive, -1 for negative, 0 for 0.

        Math.sign(10); -> 1
        Math.sign(-5); -> -1
        Math.sign(0); -> 0

13. static clamp(x, min, max) -> Clamps a number between two bounds.

        Math.clamp(15, 0, 10); -> 10
        Math.clamp(-5, 0, 10); -> 0
        Math.clamp(5, 0, 10); -> 5

14. static mod(a, b) -> Returns the absolute mod of two numbers. Different from % operator.

        Math.mod(-13, 5); -> 2

15. static toRadians(deg) -> Converts to Radian value.

        Math.toRadians(180); -> 3.14159...

16. static toDegrees(rad) -> Converts to degree value.

        Math.toDegrees(3.14159); -> 180

17. static sin(x) -> Gives sin of an angle in radians.

        Math.sin(Math.toRadians(30)); -> 0.5

18. static cos(x) -> Gives cos of an angle in radians.

        Math.cos(Math.toRadians(60)); -> 0.5

19. static tan(x) -> Gives tan of an angle in radians.

        Math.tan(Math.toRadians(45)); -> 1

20. static asin(x) -> Gives the angle which would produce the sin value.

        Math.asin(0.5); -> 0.523... (in radians)

21. static acos(x) -> Gives the angle which would produce the cos value.

        Math.acos(0.5); -> 1.047... (in radians)

22. static atan(x) -> Gives the angle which would produce the tan value.

        Math.atan(1); -> 0.785... (in radians)



General Utility functions
==========================

1. instanceOf(instance, clazz) -> Checks if an object is the instance of a class.

        instanceOf(1, Number); -> true

2. isChildOf(child, parent) -> Checks if a class extends another class directly or indirectly.

        isChildOf(NumberFormatError, RuntimeError); -> true

3. isClass(clazz) -> Checks if a value is a class.

        isClass(String); -> true
        isClass("Instance"); -> false

4. isInstance(inst) -> Checks if a value is an instance.

        isInstance(Boolean); -> true
        isInstance(true); -> false

5. getName(obj) -> Returns the original name of the class or instance.

        getName(Number); -> "Number"

Native Functions
==========================

1. asc(string) -> Returns the ascii of the first character in a string.

        asc("A"); -> 65

2. char(ascii) -> Returns the character corresponding to the given ascii.

        char(65); -> "A"

3. clock() -> Returns the current system time in milliseconds.

        clock(); -> 1.74869322457E9

4. input() -> Takes input from the console and returns it as a string.

        var a = input();

5. print(val) -> Prints value to the console.

        print("Hello");

6. println(val) -> Prints value to the console and moves to the next line.

        println("Hello");

7. println() -> Moves to the next line in console.

        println();

8. type(obj) -> Returns the type of a class, instance or function.

        type("abc"); -> "<inst 'String'>"
        type(5); -> "<inst 'Number'>"
        type(MyClass); -> "<class 'MyClass'>"
        type(MyClass()); -> "<inst 'MyClass'>"
        type(func temp(){}); -> "<fn 'temp'>"

