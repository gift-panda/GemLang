26/4/25

Just a simple tokenizer :D
follows regular rules
can distinguish between 
1) operators
2) literals
3) strings
4) keywords



Will change to context free grammer later(goes arbitarily deep)(deep thorat)
possibly change to an unrestricted grammer rule (who knows)


27/4/25

Produces an abstract syntax tree and cn visualize. Variable support will be added later(tomorrow?).

27/4/25

Made the basic interpreter work in accordance with operator precedence and associativity.

29/4/25

Added custom environments, environment nestings.
implemnted environment into variables, implemented nested environments into scopes. 
implemented blocks on the way.

30/4/25
Finally implemented functions and their closures
Added dynamically allocated lists

1/5/25
Extended support of python style dynamic reference to strings
Implemented the GNI(Gem Native Interface)

UNDER NO CIRCUMSTANCES TOUCH THE 'natives' DIRECTORY
Use: java GemNative <file path of your .java file. or directory containing multiple .java files>
To implement your native functions in gem from java
