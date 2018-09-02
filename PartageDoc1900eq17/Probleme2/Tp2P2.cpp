/*
 * Nom: compteur 32 bits
 * Copyright (C) 2005 Matthew Khouzam
 * License http://www.gnu.org/copyleft/gpl.html GNU/GPL
 * Description: Ceci est un exemple simple de programme 
 * Version: 1.1
 */


/*
*| etat present | entree D | reset | etat suivant | rouge | vert | ambree |
*|	init	   | 	X     |   1   |     init     |   1   |   0  |   0    | 
*|	init	   |    1     |   0   |     etat1    |   1   |   0  |   0    |   
*|	init	   |    0     |   0   |     init     |   1   |   0  |   0    | //a voir lutilite
*|	etat1	   | 	X     |   1   |     init     |   0   |   0  |   1    | 
*|	etat1	   |    0     |   0   |     etat2    |   0   |   0  |   1    |   
*|	etat2	   | 	X     |   1   |     init     |   0   |   1  |   0    | 
*|	etat2	   |    1     |   0   |     etat3    |   0   |   1  |   0    |   
*|	etat2	   |    0     |   0   |     init     |   0   |   1  |   0    |
*|	etat3	   | 	X     |   1   |     init     |   1   |   0  |   0    | 
*|	etat3	   |    0     |   0   |     etat4    |   1   |   0  |   0    |   
*|	etat4	   | 	X     |   1   |     init     |   0   |   0  |   0    | 
*|	etat4	   |    1     |   0   |     etat5    |   0   |   0  |   0    |   
*|	etat4	   |    0     |   0   |     init     |   0   |   0  |   0    |
*|	etat5	   | 	X     |   1   |     init     |   0   |   1  |   0    | 
*|	etat5	   |    0     |   0   |     etat6    |   0   |   1  |   0    |   
*|   etat6	   | 	X     |   X   |     init     |   1   |   0  |   0    | 
*/   
 // a revoir, mais un etat sur deux possede seulement deux scenarios


#include <avr/io.h> 

#define F_CPU 80000000
#include <util/delay.h>

int main()
{
	int compteur = 0;
	
	int relache = 0;
	
  DDRA = 0xff; // PORT A est en mode sortie
  DDRB = 0xff; // PORT B est en mode sortie
  DDRC = 0xff; // PORT C est en mode sortie
  DDRD = 0x00; // PORT D est en mode entree
  //unsigned long compteur=0; // le compteur est initialise a 0.
                            // c'est un compteur de 32 bits

  for(;;)  // boucle sans fin
  {
    switch(compteur)
    {
		case 0 : 
		compteur = 0;
		PORTA = 0b10;
		
		if(PIND != relache) 
		{
			_delay_ms(250);
			
			while(PIND != relache) // == 1 fonctionne +-bien, j'ai prefere eviter. 
			{
				PORTA = 0b10;
			_delay_ms(1);
			PORTA = 0b01;
			_delay_ms(3);
			
			}
			compteur++;
		}
		break;
		
		case 1 :
		//PORTA = 0b01;
		if(PIND == relache ) 
		{
			_delay_ms(250);
			PORTA = 0b01;
			compteur++;
		}
		break;
		
		case 2 :
		
		if(PIND != relache)
		{
			_delay_ms(250);
			PORTA = 0b10;
			compteur++;
		}
		break;
		
		case 3 :
		
		if(PIND == relache)
		{
			_delay_ms(250);
			PORTA = 0b00;
			compteur++;
		}
		break;
		
		case 4 :
		
		if (PIND != relache)
		{
			_delay_ms(250);
			PORTA = 0b01;
			compteur++;
		}
		break;
		
		case 5 :
		
		if(PIND == relache)
		{
			_delay_ms(250);
			PORTA = 0b10;
			compteur = 0;
		}	 
		break;
			 
	}
  }
  return 0; 
}
