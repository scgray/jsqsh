# Testing bulleted lists

In this test, I'll check that bulleted lists work ok with a 
variety of layouts, indent depths, and nested code elements.

  * This is a bullet at the first level `it
    has some nested code in it because, well, what the hell?`
 * We'll force this one up to the level of the previous
   one. This isn't the same behavior as github, but it is 
   a silly use case anyway.
     * This line should appear on level 2 because it is
       indented more than the previous level
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
    
    
        