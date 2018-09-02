/*
 * Nom: compteur 32 bits
 * Copyright (C) 2005 Matthew Khouzam
 * License http://www.gnu.org/copyleft/gpl.html GNU/GPL
 * Description: Ceci est un exemple simple de programme 
 * Version: 1.1
 */

| etat present | entree bouton pressoir | reset | etat prochain | sortie DEL rouge |
|    init      |         1              |   0   |      etat1    |        0         |
| 	 init      |         X  			|   1   |      init     |        0         |
|    etat1     |         1              |   0   |      etat2    |        0         |
| 	 etat1     |         X  			|   1   |      init     |        0         |
|    etat2     |         1              |   0   |      etat3    |        0         |
| 	 etat2     |         X  			|   1   |      init     |        0         |
|    etat3     |         1              |   0   |      etat4    |        0         |
| 	 etat3     |         X  			|   1   |      init     |        0         |
|    etat4     |         1              |   0   |      etat5    |        0         |
| 	 etat4     |         X  			|   1   |      init     |        0         |
| 	 etat5     |         X  			|   X   |      init     |        1         |

 
#include <avr/io.h> 

#define F_CPU 8000000
#include <util/delay.h>

int main()
{
	int compteur = 0;
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
    case 0: 
    PORTA = 0b00;
    compteur = 0;
		if(PIND &100)
		{
			_delay_ms(250);
			compteur++;		
		}
		break;
		
		case 1:
		PORTA = 0b00;
		if(PIND &100)
		{
			_delay_ms(250);
			compteur++;
		}
		break;
		
		case 2:
		PORTA = 0b00;
		if(PIND &100)
		{
			_delay_ms(250);
			compteur++;
		}
		break;
		
		case 3:
		PORTA =0b00;
		if(PIND &100)
		{
			_delay_ms(250);
			compteur++;
		}
		break;
		
		case 4:
		PORTA = 0b00;
		if(PIND &100)
		{
			_delay_ms(250);
			compteur++;
		}
		break;
		
		case 5:
		PORTA = 0b10;
		_delay_ms(1000);
		compteur = 0;
		break;
	}
  }
  return 0; 
}
