# Testing bulleted lists

In this test, I'll check that bulleted lists work ok with a 
variety of layouts, indent depths, and nested code elements.

  * This is a bullet at the first level `it
    has some nested code in it because, well, what the hell?`
 * According to github markdown, this should be at level 2, even
   though it is indented less than the previous one.
     * And this should line up with the previous one at level 2
        * And do I hear a level 3??
  * And back to level 1

And back to a regular paragraph

  * Let's test code blocks now
         this should not be a code block because it does not have
         a preceeding blank space.
  * But, now I will have a code block.

        Here is my very first code block 
        in a bulleted list
  
    text continuing the current bullet
  * And another bullet
   
        With a closing
        Code block

And a closing paragraph

  * A new bullet,
and some more that is part of the same bullet
    * And a sub-bullet
## Header 2
And starting a new paragraph

  * Bullets
  
  * With 
  
  * Blank
  
  * Lines
    
    
        