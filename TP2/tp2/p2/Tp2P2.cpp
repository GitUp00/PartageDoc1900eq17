/*
 * Nom Tp2P2.cpp
 * Auteur : Gagné S. et Sénécal J-N
 * Date : 14 septembre 2018
 * Machine a etat probleme 2 tp2
 */


/*
 * Pin de la LED connecte au PORTA(pin 1 et pin 2)
 
 
 * Machine a etat Moore
*| etat present| entree D | reset | etat suivant | rouge | vert | ambree |
*|	init	   | 	X     |   1   |     init     |   1   |   0  |   0    | 
*|	init	   |    1     |   0   |     etat1    |   1   |   0  |   0    |   
*|	init	   |    0     |   0   |     init     |   1   |   0  |   0    | 
*|	etat1	   | 	X     |   1   |     init     |   0   |   0  |   1    | 
*|	etat1	   |    0     |   0   |     etat2    |   0   |   0  |   1    |  
*|	etat1	   |    1     |   0   |     etat1    |   0   |   0  |   1    |    
*|	etat2	   | 	X     |   1   |     init     |   0   |   1  |   0    | 
*|	etat2	   |    1     |   0   |     etat3    |   0   |   1  |   0    |   
*|	etat2	   |    0     |   0   |     etat2    |   0   |   1  |   0    |
*|	etat3	   | 	X     |   1   |     init     |   1   |   0  |   0    | 
*|	etat3	   |    0     |   0   |     etat4    |   1   |   0  |   0    | 
*|	etat3	   |    1     |   0   |     etat3    |   1   |   0  |   0    |      
*|	etat4	   | 	X     |   1   |     init     |   0   |   0  |   0    | 
*|	etat4	   |    1     |   0   |     etat5    |   0   |   0  |   0    |   
*|	etat4	   |    0     |   0   |     etat4    |   0   |   0  |   0    |
*|	etat5	   | 	X     |   1   |     init     |   0   |   1  |   0    | 
*|	etat5	   |    0     |   0   |     init     |   0   |   1  |   0    | 
*|	etat5	   |    1     |   0   |     etat5    |   0   |   1  |   0    |     
*/   



#include <avr/io.h> 

#define F_CPU 80000000
#include <util/delay.h>

int main()
{
	uint8_t  compteur = 0;
	
	uint8_t relache = 0; //bouton poussoir
	
	uint8_t appuye = 0b100; // bouton poussoir
	
	uint8_t rouge = 0b10; //Led prend la couleur rouge
	
	uint8_t vert = 0b01; //Led prend la couleur verte
	
	uint8_t sansCouleur = 0b00; // Led eteinte
	
	uint8_t initCompteur = 0; // initialisation du compteur
	
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
			compteur = initCompteur;
			PORTA = rouge;
			
			if(PIND & appuye) 
			{
				_delay_ms(10);
				
				while(PIND & appuye) //couleur ambree 
				{
				PORTA = rouge;
				_delay_ms(0.5);
				PORTA = vert;
				_delay_ms(1);
			
				}
				compteur++;
			}
		break;
		
		case 1 :
		
			if(PIND == relache ) 
			{
				_delay_ms(10);
				PORTA = vert;
				compteur++;
			}
		break;
		
		case 2 :
		
			if(PIND & appuye)
			{
				_delay_ms(10);
				PORTA = rouge;
				compteur++;
			}
		break;
		
		case 3 :
		
			if(PIND == relache)
			{
				_delay_ms(10);
				PORTA = sansCouleur;
				compteur++;
			}
		break;
		
		case 4 :
		
			if (PIND & appuye)
			{
				_delay_ms(10);
				PORTA = vert;
				compteur++;
			}
		break;
		
		case 5 :
		
			if(PIND == relache)
			{
				_delay_ms(10);
				PORTA = rouge;
				compteur = initCompteur;
			}	 
		break;
			 
	}
  }
  return 0; 
}
