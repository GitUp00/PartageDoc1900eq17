// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)

#include <avr/interrupt.h>
#include <avr/io.h>
#define F_CPU 8000000
#include <util/delay.h>
#include "memoire_24.h"





int main()
{
	Memoire24CXXX entete;
	
	
    DDRA = 0xff;
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    
    char polyLecture[44] ;
    /*
    lecture(const uint16_t adresse, uint8_t *donnee,
                   const uint8_t longueur);
                   
    ecriture(const uint16_t adresse, uint8_t *donnee,
                    const uint8_t longueur);
    */
    
    
    bool vrai = true;
    
    char polyEcriture[] = "*P*O*L*Y*T*E*C*H*N*I*Q*U*E* *M*O*N*T*R*E*A*L*" ;
    entete.ecriture(0x00, (uint8_t*)polyEcriture,sizeof(polyEcriture));
    
    _delay_ms(5);
    
    entete.lecture(0x00,(uint8_t*)polyLecture,sizeof(polyLecture));
    for (int i = 0; i < 44; i++)
	{
		if (polyEcriture[i] == polyLecture[i])
		{
			vrai = true;
		}
	}
	
	if (vrai)
	{
		PORTA = 0b01;
	}
	else
		PORTA = 0b10;
	
}


    
  
